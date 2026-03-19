{
  description = "jni build environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=fe416aaedd397cacb33a610b33d60ff2b431b127";
    flake-parts.url = "github:hercules-ci/flake-parts";
    clang-msvc-cross.url = "git+https://codeberg.org/silenium-dev/clang-msvc-cross.git";
  };

  outputs = { flake-parts, nixpkgs, clang-msvc-cross, ... } @ inputs: flake-parts.lib.mkFlake { inherit inputs; } {
    imports = [ ];
    flake = {
      lib =
        let
          pkgs = import nixpkgs {
            system = "x86_64-linux";
          };
        in
        rec {
          isWindows = targetSystem: (osOf targetSystem) == "windows";
          isLinux = targetSystem: (osOf targetSystem) == "linux";
          osOf = targetSystem: builtins.elemAt (pkgs.lib.strings.split "-" targetSystem) 2;
          archOf = targetSystem: builtins.elemAt (pkgs.lib.strings.split "-" targetSystem) 0;
          linuxFfmpegArchOf = targetSystem:
            let
              arch = builtins.elemAt (pkgs.lib.strings.split "-" targetSystem) 0;
            in
            if arch == "x86_64" then "64"
            else if arch == "aarch64" then "arm64"
            else throw "Unsupported architecture ${arch}";
          systemNativesMapping = {
            "x86_64-linux" = "linux-x86_64";
            "aarch64-linux" = "linux-arm64";
            "x86_64-windows" = "windows-x86_64";
            "aarch64-windows" = "windows-arm64";
          };

          targetPkgs = target: {
            "x86_64-linux" = pkgs;
            "aarch64-linux" = pkgs.pkgsCross.aarch64-multiplatform;
            "x86_64-windows" = pkgs.pkgsCross.x86_64-windows;
            "aarch64-windows" = pkgs.pkgsCross.aarch64-windows;
          }."${target}";
          targetBuildDeps = target:
            if isLinux target then
              let
                pkgs = targetPkgs target;
              in
              with pkgs; [
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
              ]
            else [ ];
          nativeBuildDeps = target:
            [
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
              pkgs.wineWow64Packages.stable
            ] ++ (
              if isLinux target then
                let
                  crossPkgs = targetPkgs target;
                in
                [
                  # C/C++ Toolchain
                  crossPkgs.gcc
                  crossPkgs.binutils
                  crossPkgs.pkg-config
                ]
              else [ ]
            );

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
            , preUnpackPhase ? sys: ":"
            , postUnpackPhase ? sys: ":"
            , unpack ? sys: null
            , patch ? sys: null
            , preConfigurePhase ? sys: ""
            , postConfigurePhase ? sys: ""
            , preInstallPhase ? sys: ":"
            , postInstallPhase ? sys: ":"
            , additionalNativeInputs ? targetSystem: pkgs: [ ]
            , additionalInputs ? targetSystem: pkgs: [ ]
            }:
            let
              forAllSystems = f:
                let
                  archResults = (builtins.listToAttrs (map
                    (targetSystem: {
                      name = targetSystem;
                      value = f targetSystem;
                    })
                    targetSystems));
                in
                {
                  "${name}" = pkgs.linkFarm "${name}" (pkgs.lib.concatMapAttrs
                    (k: v: {
                      "${systemNativesMapping."${k}"}" = v;
                    })
                    archResults);
                } // (pkgs.lib.concatMapAttrs (k: v: { "${name}-${k}" = v; }) archResults);
              compiledLibName = targetSystem: name:
                if isWindows targetSystem then "${name}.dll"
                else "lib${name}.so";
              outLibName = targetSystem: name:
                if isWindows targetSystem then "${name}.dll"
                else "lib${name}.so";
              mkDerivation = targetSystem:
                if isWindows targetSystem
                then clang-msvc-cross.lib.mkDerivation (archOf targetSystem)
                else pkgs.stdenv.mkDerivation;
            in
            forAllSystems (targetSystem: (mkDerivation targetSystem) rec {
              pname = name + "-${targetSystem}";
              inherit version;
              srcs = sources targetSystem;

              postUnpack = postUnpackPhase targetSystem;
              unpackPhase = unpack targetSystem;
              preUnpack = preUnpackPhase targetSystem;

              patchPhase = patch targetSystem;

              buildInputs = (targetBuildDeps targetSystem) ++ (additionalInputs targetSystem (targetPkgs targetSystem));
              nativeBuildInputs = (nativeBuildDeps targetSystem) ++ (additionalNativeInputs targetSystem pkgs);
              sourceRoot = ".";

              mesonFlags = [
                "--cross-file=${if isWindows targetSystem then "$CROSS_FILE" else ./cross/${targetSystem}.ini}"
                "--buildtype=${buildType}"
                "-Dwrap_mode=forcefallback"
                "-Ddefault_library=static"
                "-Dprefer_static=true"
              ];
              buildDir = "build-${targetSystem}";

              configurePhase = (pkgs.lib.strings.join "\n" [
                (preConfigurePhase targetSystem)
                "meson setup ${pkgs.lib.strings.join " " mesonFlags} ${buildDir} || (cat '${buildDir}/meson-logs/*' && exit 1)"
                (postConfigurePhase targetSystem)
              ]);

              buildPhase = ''
                meson compile -v -C ${buildDir} ${mesonTarget}
              '';

              fullLibPath = pkgs.lib.strings.join "/" (
                [ buildDir ]
                ++ pkgs.lib.optional (libDir != null && libDir != "") libDir
                ++ [ (compiledLibName targetSystem libName) ]
              );

              installPhase = (pkgs.lib.strings.join "\n" [
                (preInstallPhase targetSystem)
                ''
                  mkdir -p $out
                  cp ${fullLibPath} $out/${outLibName targetSystem libName}
                ''
                (postInstallPhase targetSystem)
              ]);
            });
        };
    };

    systems = [ "x86_64-linux" ];
  };
}
