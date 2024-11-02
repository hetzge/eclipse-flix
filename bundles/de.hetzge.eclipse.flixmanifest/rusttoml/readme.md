extism gen plugin -l Rust -o plugin

cargo add extism-pdk

[lib]
crate_type = ["cdylib"]


rustup target add wasm32-wasip1
rustup target add wasm32-unknown-unknown

cargo build --target wasm32-unknown-unknown --release

https://www.wa2.io/imports