{
  description = "A Nix-flake-based Java development environment";

  inputs.nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      javaVersion = 23; # Change this value to update the whole stack

      supportedSystems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];

      # Define the overlay
      javaOverlay = final: prev: rec {
        jdk = prev."jdk${toString javaVersion}";
        maven = prev.maven.override { jre = jdk; };
        gradle = prev.gradle.override { java = jdk; };
      };

      # Use the overlay when importing nixpkgs
      forEachSupportedSystem = f: nixpkgs.lib.genAttrs supportedSystems (system: f {
        pkgs = import nixpkgs { inherit system; overlays = [ javaOverlay ]; };
      });
    in
    {
      # Export the overlay
      overlays.default = javaOverlay;

      devShells = forEachSupportedSystem ({ pkgs }: {
        default = pkgs.mkShell {
          packages = with pkgs; [ jdk gradle graphviz gnuplot maelstrom-clj jbang ];
          shellHook = ''
            export JAVA_HOME=${pkgs.jdk}/lib/openjdk
          '';
        };
      });
    };
}
