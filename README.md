# Biff example project

## Install Clojure

[Instructions here](https://clojure.org/guides/install_clojure#_mac_os_instructions).

TLRD (install java and clj tools)

```sh
brew tap homebrew/cask-versions
brew install --cask temurin17
java --version # check that java is installed
# might need to add java to your path explicitly
echo 'export PATH="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin:$PATH"' >> ~/.zshrc

brew install clojure/tools/clojure
```

## Install babashka

```sh
brew install bb
```

## Start the app

Run `bb dev` to get started. See `bb tasks` for other commands.
