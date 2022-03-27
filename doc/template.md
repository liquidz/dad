# template

Create a text file from the specified template files.

| Key | Value | Required | Default |
| --- | ----- | -------- | ------- |
| path | String and (not= "") | Yes |  |
| source | String and (not= "") | Yes |  |
| variables | Map | No |  |
| mode | String and (not= "") | No |  |
| owner | String and (not= "") | No |  |
| group | String and (not= "") | No |  |

Examples
```clojure
(template {:path "result.txt" :source "source.txt" :variables {:msg "world"}})
```