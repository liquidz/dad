# package

Install packages.

| Key | Value | Required | Default |
| --- | ----- | -------- | ------- |
| name | String and (not= "") or [String and (not= "")] | Yes |  |
| action | #{:install, :remove, :delete, :uninstall, "install", "remove", "delete", "uninstall"} | No | :install |

Examples
```clojure
(package {:name "vim"})
```