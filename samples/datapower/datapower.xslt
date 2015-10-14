<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dp="http://www.datapower.com/extensions"
                xmlns:dpconfig="http://www.datapower.com/param/config"
                extension-element-prefixes="dp"
                exclude-result-prefixes="dp dpconfig">
  <xsl:output method="xml"/>
  <xsl:template match="/">
    <tracking_event xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:noNamespaceSchemaLocation="wp.xsd">
      <HostName>ix52</HostName>
      <HostInfo>
        <xsl:value-of select="dp:variable('var://service/system/ident')" />
      </HostInfo>
      <Service>
        <xsl:value-of select="dp:variable('var://service/processor-name')" />
      </Service>
      <Domain>
        <xsl:value-of select="dp:variable('var://service/domain-name')" />
      </Domain>
      <Policy>
        <xsl:value-of select="dp:variable('var://service/transaction-policy-name')" />
      </Policy>
      <Rule>
        <xsl:value-of select="dp:variable('var://service/transaction-rule-name')" />
      </Rule>
      <EventType>RECEIVE</EventType>
      <Signature>
        <xsl:value-of select="dp:generate-uuid()" />
      </Signature>
      <Tag>
        <xsl:value-of select="dp:variable('var://service/transaction-id')" />
      </Tag>
      <StartTime datatype="Timestamp" units="Milliseconds">
        <xsl:value-of select="dp:time-value()" />
      </StartTime>
      <ResponseMode>
        <xsl:value-of select="dp:variable('var://service/response-mode')" />
      </ResponseMode>
      <ErrorCode>
        <xsl:value-of select="dp:variable('var://service/error-code')" />
      </ErrorCode>
      <ErrorSubCode>
        <xsl:value-of select="dp:variable('var://service/error-subcode')" />
      </ErrorSubCode>
      <ErrorMsg>
        <xsl:value-of select="dp:variable('var://service/formatted-error-message')" />
      </ErrorMsg>
      <MsgData format="string">
        <xsl:value-of select="."/>
      </MsgData>
    </tracking_event>
  </xsl:template>
</xsl:stylesheet>