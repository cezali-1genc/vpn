# Vendored WireGuard Tunnel Library

This module vendors the official `WireGuard/wireguard-android` tunnel module at release tag
`1.0.20231018` (Apache License 2.0). Its native components are pinned to the submodule
revisions declared by that release:

- `wireguard-tools`: `b4f6b4f229d291daf7c35c6f1e7f4841cc6d69bc` (GPL-2.0)
- `termux-elf-cleaner`: `7efc05090675ec6161b7def862728086a26c3b1f` (GPL-3.0-or-later)

The upstream library sources and license files are retained under this directory. Native
builds use the Android NDK and Go 1.21.3; CI provisions Go separately. This is source vendoring,
not a prebuilt or mock tunnel implementation.

Upstream: https://github.com/WireGuard/wireguard-android
