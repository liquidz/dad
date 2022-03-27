# directory

Create directories

| Key | Value | Required | Default |
| --- | ----- | -------- | ------- |
| path | String and (not= "") | Yes |  |
| action | #{:create, :delete, :remove, "create", "delete", "remove"} | No | :create |
| mode | String and (not= "") | No |  |
| owner | String and (not= "") | No |  |
| group | String and (not= "") | No |  |

Examples
```clojure
(directory {:path "foo/bar"})
```