## Nagios setup (Linux)

Download `nagios2json` [plugin](https://github.com/macskas/nagios2json).

Build and install as described in documentation:

1. Run `make`
2. rename build binary to 'nagios2json.cgi' (May not need this step)
3. Copy built binary to Nagios "cgi-bin" folder (may be `/usr/lib/nagios/cgi-bin`)

## Streaming configuration

1. Fallow the instruction on TNT4J-Streams

2. Edit `tnt-data-source.xml` parser configuration file and change: your Nagios server IP (url), username and password and `nagios2json`
parameters i.e. `servicestatustypes=31`

```xml
    <step name="Step 1"
        url="http://[YOUR_NAGIOS_SERVER_IP]/nagios/cgi-bin/nagios2json.cgi?servicestatustypes=31"
        method="GET"
        username="myNagiosUserName"
        password="myNagiosUserSecretPassword">
        <schedule-cron expression="0/15 * * * * ? *" />
    </step>
```

3. If needed change fields mapping for reported data to comply your needs. 

## Sample report data
 
Sample report data is available in `report.json` file. 