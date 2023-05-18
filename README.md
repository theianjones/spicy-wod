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
brew install borkdude/brew/babashka
```

## Copy config-template.edn to config.edn

```sh 
cp config-template.edn config.edn
```

## Start the app

Run `bb dev` to get started. See `bb tasks` for other commands.

## Install Calva

Add the `Calva` extention to vscode (assuming you don't want to code in Emacs).

Once the app is started, hit `CMD+SHIFT+P` then type "Calva: Connect" (or `CTRL+OPTION+C CTRL+OPTION+C`) then select `deps.edn` from the dropdown menu.

Now the repl is connected and you cant hit `OPTION+ENTER` to evaluate s-expressions.

## Add default data to DB

Connect to the repl.

Then head to `src/com/spicy/repl.clj` and evaluate the file (`CTRL+OPTION+C ENTER`)

Move your cursor to line 28 and evaluate the form `OPTION+ENTER`.

Now you will have 3 users, some workouts, and 2 workout results for each user in your database.

## Logging in

Type an email into the sign up or sign in form then take the URL that's printed out in your terminal and paste it into the browser.
