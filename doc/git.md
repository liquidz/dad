# git

Execute `git` command.

| Key | Value | Required | Default |
| --- | ----- | -------- | ------- |
| path | String and (not= "") | Yes |  |
| url | String and (not= "") | Yes |  |
| revision | String and (not= "") | No | main |
| mode | String and (not= "") | No |  |
| owner | String and (not= "") | No |  |
| group | String and (not= "") | No |  |

Examples
```clojure
(git {:path "dad-source" :url "https://github.com/liquidz/dad"})
```