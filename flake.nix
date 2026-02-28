{
  description = "jni build environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem
      (system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
          baseEnvs = {
            crossBuildDeps = pkgBase: with pkgBase; if pkgBase.stdenv.hostPlatform.system != "x86_64-windows" then [
              # Libraries
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
            ] else [ ];
            crossBuildTools = pkgBase: with pkgBase; [
              gcc
              binutils
              pkg-config
            ];
            nativeBuildTools = with pkgs; [
              # Java Development
              jdk21
              gradle_9

              # C/C++ Build Toolchain
              cmake
              ninja
              meson

              # Additional utilities
              git
              python3
              perl
            ];
          };
        in
        {
          lib = rec {
            inherit system;
            inherit (baseEnvs) crossBuildDeps crossBuildTools nativeBuildTools;

            targetCrossPkgs = pkgs: target:
              if target == "aarch64-linux" then pkgs.pkgsCross.aarch64-multiplatform
              else if target == "x86_64-linux" then pkgs
              else if target == "x86_64-windows" then pkgs.pkgsCross.mingwW64
              else throw "Invalid target ${target}";

            baseSetup = { pkgs, target ? system, extraNativeBuildInputs ? [ ], extraBuildInputs ? [ ] }:
              let
                crossPkgs = (targetCrossPkgs pkgs target);
              in
              {
                strictDeps = true;
                nativeBuildInputs = baseEnvs.nativeBuildTools
                  ++ (baseEnvs.crossBuildTools crossPkgs)
                  ++ extraNativeBuildInputs;
                buildInputs = (baseEnvs.crossBuildDeps crossPkgs) ++ extraBuildInputs;

                JAVA_HOME = "${pkgs.jdk21}";
                CMAKE_GENERATOR = "Ninja";
              };

            mkDevShell = { pkgs, target ? system, extraNativeBuildInputs ? [ ], extraBuildInputs ? [ ] }:
              pkgs.mkShell
                ({
                  shellHook = ''
                    echo "Development environment loaded"
                    echo "Java version: $(java -version 2>&1 | head -n 1)"
                    echo "Gradle version: $(gradle --version | grep Gradle)"
                    echo "CMake version: $(cmake --version | head -n 1)"
                    echo "GCC version: $(gcc --version | head -n 1)"
                    echo "Ninja version: $(ninja --version)"
                  '';
                } // baseSetup { inherit pkgs target extraNativeBuildInputs extraBuildInputs; });

            buildJNILib = { name, version, source, pkgs, target ? system, extraNativeBuildInputs ? [ ], extraBuildInputs ? [ ] }:
              pkgs.stdenv.mkDerivation
                ({
                  inherit name version source;

                  buildPhase = ''
                    echo "Building..."
                  '';
                } // baseSetup { inherit pkgs target extraNativeBuildInputs extraBuildInputs; });
          };
          devShells = {
            default = self.lib.${system}.mkDevShell { inherit pkgs; };
            linux-arm64 = self.lib.${system}.mkDevShell {
              inherit pkgs;
              target = "aarch64-linux";
              extraNativeBuildInputs = with pkgs; [ qemu-user ];
            };
            linux-x86_64 = self.lib.${system}.mkDevShell {
              inherit pkgs;
            };
            windows-x86_64 = self.lib.${system}.mkDevShell {
              inherit pkgs;
              target = "x86_64-windows";
              extraNativeBuildInputs = with pkgs; [ wineWow64Packages.staging ];
            };
          };
        }
      );
}
