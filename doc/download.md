# download

Download a file from remote host.
This resource requires `curl` command.

| Key | Value | Required | Default |
| --- | ----- | -------- | ------- |
| path | String and (not= "") | Yes |  |
| url | String and (not= "") | Yes |  |
| mode | String and (not= "") | No |  |
| owner | String and (not= "") | No |  |
| group | String and (not= "") | No |  |

Examples
```clojure
(download {:path "foobar" :url "https://example.com"})
```