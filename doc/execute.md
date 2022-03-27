# execute

Execute a shell command.

| Key | Value | Required | Default |
| --- | ----- | -------- | ------- |
| command | String and (not= "") or [String and (not= "")] | Yes |  |
| cwd | String and (not= "") | No |  |
| pre | String and (not= "") | No |  |
| pre-not | String and (not= "") | No |  |

Examples
```clojure
(execute {:cwd "/tmp" :command "curl -sfLo foobar https://example.com"})

(execute {:command "touch foo" :pre-not "test -e bar"})
```