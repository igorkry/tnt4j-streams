## Using private key authentication

In case you'd like to use private key authentication you may generate private keys in *nix using command:
```bash
    ssh-keygen
```

This command generates `id_rsa` file in your home directory e.g. `~/.ssh/id_rsa`

Having this generated key file: 
* on TNT4J-Streams runner machine copy key file to e.g. `[streams root dir]/config/id_rsa`
* on files serving machine add key file to  `~/.ssh/authorized_keys` by using command:
```bash
    cat id_rsa >> .ssh/authorized_keys
```

To setup TNT4J-Streams to use generated key, use `FileSystemLineStream` option:
```xml
    <property name="IdentityFromPrivateKey" value="../config/id_rsa"/>
```
