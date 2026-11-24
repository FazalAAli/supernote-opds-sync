{
  description = "Supernote OPDS Sync: Android dev shell";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" "x86_64-darwin" ];
      forAllSystems = f: nixpkgs.lib.genAttrs systems (system: f (import nixpkgs {
        inherit system;
        config = { allowUnfree = true; android_sdk.accept_license = true; };
      }));
    in
    {
      devShells = forAllSystems (pkgs:
        let
          buildTools = "35.0.0";
          sdk = (pkgs.androidenv.composeAndroidPackages {
            buildToolsVersions = [ buildTools ];
            platformVersions = [ "35" ];
            includeEmulator = false;
            includeSystemImages = false;
            includeNDK = false;
          }).androidsdk;
          home = "${sdk}/libexec/android-sdk";
        in
        {
          default = pkgs.mkShell {
            packages = [ sdk pkgs.jdk17 pkgs.gradle pkgs.android-tools ];
            ANDROID_HOME = home;
            # Override any preset value (GitHub runners set one); AGP fails if the two disagree.
            ANDROID_SDK_ROOT = home;
            # The aapt2 that AGP downloads from Maven is dynamically linked and won't run on NixOS.
            GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${home}/build-tools/${buildTools}/aapt2";
          };
        });
    };
}
