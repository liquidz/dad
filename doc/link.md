# link

Create a symbolic link.

| Key | Value | Required | Default |
| --- | ----- | -------- | ------- |
| path | String and (not= "") | Yes |  |
| to | String and (not= "") | Yes |  |

Examples
```clojure
(link {:path "~/.lein/profiles.clj" :to "/path/to/your/dotfiles/profiles.clj"})
```