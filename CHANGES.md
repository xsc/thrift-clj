## Changes: `thrift-clj`

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
