#!/usr/bin/env dad --no-color --dry-run

(def vars
  {:src-dir "/usr/local/src/vim"
   :configure (->> ["--prefix=/usr/local"
                    "--with-features=huge"
                    "--enable-multibyte"
                    "--enable-python3interp"
                    "--enable-fail-if-missing"]
                   (str/join " "))
   :packages ["build-essential"
              "git"
              "libncurses5-dev"
              "python3-dev"
              "cmake"
              "libboost-all-dev"
              "sudo"]})

(def render #(dad/render % vars))

(let [{:keys [src-dir packages configure]} vars]
  (package "vim" {:action :uninstall})
  (package packages)

  (git {:path src-dir :url "https://github.com/vim/vim"})

  (execute {:cwd src-dir
            :command (render "./configure {{configure}}")
            :pre-not "test -e src/auto/config.log"})

  (execute {:cwd src-dir
            :command "make && make install"
            :pre-not "test -e /usr/local/bin/vim"})

  (template {:path (render "{{src-dir}}/rebuild.sh")
             :source "rebuild.sh.tmpl"
             :variables vars
             :mode "755"}))
