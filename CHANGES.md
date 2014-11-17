## Changes: `thrift-clj`

### 0.3.0

- drop compatibility with Clojure 1.4.0.
- upgrade base Clojure version to 1.6.0.
- upgrade dependencies.
- exclude clojure.core functions from auto-generated namespaces.

### 0.2.1

- upgrade libthrift to 0.9.1.

### 0.2.0

- replaced log4j with logback, moved to `:dev` profile (thanks to @bts, pull request #3)

### 0.1.2

- testing against different Clojure versions
- fixes exception when setting a non-optional bool field to `false` (thanks to @bts, pull request #1)
- refer map factory functions '->Type' and 'map->Type' when importing a Thrift type (thanks to @bts, pull request #2)

### 0.1.1

- moved client/server transports into `thrift-clj.transports`
- moved client functions into `thrift-clj.client` (was: `thrift-clj.client.core`)
- moved server functions into `thrift-clj.server` (was: `thrift-clj.server.core`)
- added `framed`/`fast-framed` client transports for use with `nonblocking-server`
- renamed `thrift-clj.thrift.types/type-metadata` to `thrift-type-metadata`

### 0.1.0

- Initial Release
