## Installation

1. Fallow the instruction on TNT4J-Streams

2. Edit `tnt-data-source.xml` parser configuration file and change: your Nagios server IP (url), username and password

```xml
    <step name="Step 1"
        url="http://[YOUR_NAGIOS_SERVER_IP]/nagios/cgi-bin/nagios2json.cgi?servicestatustypes=31"
        method="GET"
        username="myUserName"
        password="myUserSecretPassword">
        <schedule-cron expression="0/15 * * * * ? *" />
    </step>
```

## Nagios setup (Linux)

Download plugin: https://github.com/macskas/nagios2json

Build and install as described in documentation:

1. Run `make`
2. rename build binary to 'nagios2json.cgi' (May not need this step)
3. Copy built binary to Nagios "cgi-bin" folder (may be `/usr/lib/nagios/cgi-bin`)


