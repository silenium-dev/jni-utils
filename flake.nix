{
  description = "jni build environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
  };

  outputs = { flake-parts, nixpkgs, ... } @ inputs: flake-parts.lib.mkFlake { inherit inputs; } {
    imports = [ ];
    flake = {
      lib =
        let
          pkgs = nixpkgs.legacyPackages."x86_64-linux";
        in
        rec {
          osOf = targetSystem: builtins.elemAt (pkgs.lib.strings.split "-" targetSystem) 2;
          archOf = targetSystem: builtins.elemAt (pkgs.lib.strings.split "-" targetSystem) 0;
          linuxFfmpegArchOf = targetSystem:
            let
              arch = builtins.elemAt (pkgs.lib.strings.split "-" targetSystem) 0;
            in
            if arch == "x86_64" then "64"
            else if arch == "aarch64" then "arm64"
            else throw "Unsupported architecture ${arch}";

          targetPkgs = target:
            if target == "x86_64-linux" then pkgs
            else if target == "aarch64-linux" then pkgs.pkgsCross.aarch64-multiplatform
            else if target == "x86_64-windows" then pkgs.pkgsCross.mingwW64
            else throw "Unsupported target ${target}";
          targetBuildDeps = target:
            let
              pkgs = targetPkgs target;
            in
            if target == "x86_64-windows" then [ ]
            else with pkgs; [
              libGL
              mesa-gl-headers
              libdrm
              libx11
              libva
              libdovi
              libdrm
              libva
              systemdLibs
              hwdata
            ];
          nativeBuildDeps = target:
            let
              crossPkgs = targetPkgs target;
            in
            [
              # C/C++ Toolchain
              crossPkgs.gcc
              crossPkgs.binutils
              crossPkgs.pkg-config

              # Java Development
              pkgs.jdk21
              pkgs.gradle_9

              # C/C++ Build Tools
              pkgs.cmake
              pkgs.ninja
              pkgs.meson

              # Additional utilities
              pkgs.git
              pkgs.python3
              pkgs.perl
              pkgs.gnused
              pkgs.qemu-user
            ];

          buildJNILib =
            { name
            , version ? "0.1.0"
            , sources ? targetSystem: [ ]
            , targetSystems ? [
                "x86_64-linux"
                "aarch64-linux"
                "x86_64-windows"
              ]
            , mesonTarget
            , buildType ? "debug"
            , libName
            , libDir ? null
            , preConfigurePhase ? sys: ""
            , postConfigurePhase ? sys: ""
            , postUnpackPhase ? sys: ":"
            , unpack ? sys: null
            , patch ? sys: null
            , preUnpackPhase ? sys: ":"
            , additionalNativeInputs ? [ ]
            , additionalInputs ? [ ]
            }:
            let
              forAllSystems = f:
                let
                  archResults = (builtins.listToAttrs (map
                    (targetSystem: {
                      name = "${name}-${targetSystem}";
                      value = f targetSystem;
                    })
                    targetSystems));
                in
                {
                  default = pkgs.linkFarmFromDrvs "${name}" (builtins.attrValues archResults);
                } // archResults;
              fullLibFile = targetSystem: name:
                if targetSystem == "x86_64-windows" then "lib${name}.dll"
                else "lib${name}.so";
            in
            forAllSystems (targetSystem: pkgs.stdenv.mkDerivation rec {
              pname = name + "-${targetSystem}";
              inherit version;
              srcs = sources targetSystem;

              postUnpack = postUnpackPhase targetSystem;
              unpackPhase = unpack targetSystem;
              preUnpack = preUnpackPhase targetSystem;

              patchPhase = patch targetSystem;

              buildInputs = (targetBuildDeps targetSystem) ++ additionalInputs;
              nativeBuildInputs = (nativeBuildDeps targetSystem) ++ additionalNativeInputs;
              sourceRoot = ".";

              mesonFlags = [
                "--cross-file=${./cross/${targetSystem}.ini}"
                "--buildtype=${buildType}"
              ];
              buildDir = "build-${targetSystem}";

              configurePhase = (pkgs.lib.strings.join "\n" [
                (preConfigurePhase targetSystem)
                "meson setup ${pkgs.lib.strings.join " " mesonFlags} ${buildDir} || (cat '${buildDir}/meson-logs/*' && exit 1)"
                (postConfigurePhase targetSystem)
              ]);

              buildPhase = ''
                meson compile -C ${buildDir} ${mesonTarget}
              '';

              fullLibPath = pkgs.lib.strings.join "/" (
                [ buildDir ]
                ++ pkgs.lib.optional (libDir != null && libDir != "") libDir
                ++ [ (fullLibFile targetSystem libName) ]
              );

              installPhase = ''
                mkdir -p $out/lib
                mv ${fullLibPath} $out/lib
              '';
            });
        };
    };

    systems = [ "x86_64-linux" ];
  };
}
