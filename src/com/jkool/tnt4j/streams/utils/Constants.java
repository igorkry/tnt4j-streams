/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.utils;

/**
 * Global Constants
 *
 * @version $Revision: 35 $
 */
public final class Constants
{
  public static final String LINE_SEPARATOR = System.getProperty ("line.separator");
  public static final String NASTEL_16x16_ICON_FILE = "NastelIcon-16x16.png";

  // Explorer Global Settings record id
  public static final String TWEX_SYSTEM_DEFAULTS_ID = "TWEAppSystemDefault";

  public static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>";
  //////////////////////////////////////////////////////////////////////////////////////
  //
  // Probe transaction data XML definitions

  // XML schema version
  public static final int XML_MSG_VERSION = 2;

  // XML elements
  public static final String XML_NAS_JANUS_LABEL = "nas_tw";
  public static final String XML_NAS_SERVER_LABEL = "nas_svr";
  public static final String XML_NAS_APPLICATION_LABEL = "nas_app";
  public static final String XML_NAS_SESSION_LABEL = "nas_sess";
  public static final String XML_NAS_RESMGR_LABEL = "nas_rsmgr";
  public static final String XML_NAS_LUW_LABEL = "nas_luw";
  public static final String XML_NAS_CLIENT_USER_LABEL = "nas_cu";
  public static final String XML_NAS_MESSAGE_LABEL = "nas_msg";
  public static final String XML_NAS_MSG_STRDATA_LABEL = "nas_ms_str";
  public static final String XML_NAS_MSG_BINDATA_LABEL = "nas_ms_bin";
  public static final String XML_NAS_OPERATION_LABEL = "nas_op";
  public static final String XML_NAS_PROPERTY_LABEL = "nas_prop";

  // General attributes
  public static final String XML_VERSION_LABEL = "tw_ver";
  public static final String XML_PROBE_TYPE_LABEL = "tw_prb";

  // Server element attributes
  public static final String XML_SERVER_NAME_LABEL = "sv_name";
  public static final String XML_SERVER_IP_LABEL = "sv_ip";
  public static final String XML_SERVER_OS_LABEL = "sv_os";
  public static final String XML_SERVER_CPU_COUNT_LABEL = "sv_cpus";
  public static final String XML_SERVER_MIPS_COUNT_LABEL = "sv_mips";

  // Application element attributes
  public static final String XML_APPL_NAME_LABEL = "ap_name";
  public static final String XML_APPL_USER_LABEL = "ap_user";
  public static final String XML_APPL_URL_LABEL = "ap_url";

  // Resource Manager element attributes
  public static final String XML_RESMGR_NAME_LABEL = "rm_name";
  public static final String XML_RESMGR_TYPE_LABEL = "rm_type";
  public static final String XML_RESMGR_SERVER_LABEL = "rm_svr";

  // Session element attributes
  public static final String XML_SESSION_SIGNATURE_LABEL = "ss_sig";
  public static final String XML_SESSION_TAG_LABEL = "ss_tag";
  public static final String XML_SESSION_START_TIME_SEC_LABEL = "ss_str_s";
  public static final String XML_SESSION_START_TIME_USEC_LABEL = "ss_str_u";
  public static final String XML_SESSION_END_TIME_SEC_LABEL = "ss_end_s";
  public static final String XML_SESSION_END_TIME_USEC_LABEL = "ss_end_u";

  // LUW element attributes
  public static final String XML_LUW_SIGNATURE_LABEL = "lu_sig";
  public static final String XML_LUW_TYPE_LABEL = "lu_type";
  public static final String XML_LUW_STATUS_LABEL = "lu_stat";
  public static final String XML_LUW_PID_LABEL = "lu_pid";
  public static final String XML_LUW_TID_LABEL = "lu_tid";
  public static final String XML_LUW_START_TIME_SEC_LABEL = "lu_str_s";
  public static final String XML_LUW_START_TIME_USEC_LABEL = "lu_str_u";
  public static final String XML_LUW_END_TIME_SEC_LABEL = "lu_end_s";
  public static final String XML_LUW_END_TIME_USEC_LABEL = "lu_end_u";
  public static final String XML_LUW_EXCEPTION_LABEL = "lu_exc";

  // Client User element attributes
  public static final String XML_CLNTUSR_NAME_LABEL = "cu_user";
  public static final String XML_CLNTUSR_IP_LABEL = "cu_ip";
  public static final String XML_CLNTUSR_RES_LOC_LABEL = "cu_res";
  public static final String XML_CLNTUSR_TIME_SEC_LABEL = "cu_time_s";
  public static final String XML_CLNTUSR_TIME_USEC_LABEL = "cu_time_u";
  public static final String XML_CLNTUSR_PROTOCOL_LABEL = "cu_prot";
  public static final String XML_CLNTUSR_PARAM_LABEL = "cu_param";
  public static final String XML_CLNTUSR_PROGRAM_LABEL = "cu_pgrm";
  public static final String XML_CLNTUSR_OS_LABEL = "cu_os";
  public static final String XML_CLNTUSR_REFERER_LABEL = "cu_rfr";

  // Message element attributes
  public static final String XML_MSG_SIGNATURE_LABEL = "ms_sig";
  public static final String XML_MSG_TAG_LABEL = "ms_tag";
  public static final String XML_MSG_CORRELATOR_LABEL = "ms_cor";
  public static final String XML_MSG_SIZE_LABEL = "ms_size";
  public static final String XML_MSG_FORMAT_LABEL = "ms_fmt";
  public static final String XML_MSG_TYPE_LABEL = "ms_type";
  public static final String XML_MSG_TRANSPORT_LABEL = "ms_trp";
  public static final String XML_MSG_VALUE_LABEL = "ms_val";

  // Message Data element attributes
  public static final String XML_MSG_DATA_LABEL = "ms_data";

  // Operation element attributes
  public static final String XML_OP_FUNC_LABEL = "op_func";
  public static final String XML_OP_TYPE_LABEL = "op_type";
  public static final String XML_OP_CC_LABEL = "op_cc";
  public static final String XML_OP_RC_LABEL = "op_rc";
  public static final String XML_OP_RES_NAME_LABEL = "op_rsc";
  public static final String XML_OP_RES_TYPE_LABEL = "op_rsc_t";
  public static final String XML_OP_USER_NAME_LABEL = "op_user";
  public static final String XML_OP_START_TIME_SEC_LABEL = "op_str_s";
  public static final String XML_OP_START_TIME_USEC_LABEL = "op_str_u";
  public static final String XML_OP_END_TIME_SEC_LABEL = "op_end_s";
  public static final String XML_OP_END_TIME_USEC_LABEL = "op_end_u";
  public static final String XML_OP_ELAPSED_TIME_LABEL = "op_elps_u";
  public static final String XML_OP_MSG_AGE_LABEL = "op_mage_u";
  public static final String XML_OP_IDLE_TIME_LABEL = "op_idle_u";
  public static final String XML_OP_STEP_LABEL = "op_step";
  public static final String XML_OP_LEVEL_LABEL = "op_lvl";
  public static final String XML_OP_EXCEPTION_LABEL = "op_exc";
  public static final String XML_OP_SEVERITY_LABEL = "op_sev";
  public static final String XML_OP_LOCATION_LABEL = "op_loc";
  public static final String XML_OP_CORRELATOR_LABEL = "op_cor";

  // Property element attributes
  public static final String XML_PROPERTY_TYPE_LABEL = "pr_type";
  public static final String XML_PROPERTY_VALUE_LABEL = "pr_val";
  public static final String XML_PROPERTY_TIME_SEC_LABEL = "pr_tm_s";
  public static final String XML_PROPERTY_TIME_USEC_LABEL = "pr_tm_u";
  //////////////////////////////////////////////////////////////////////////////////////
  //
  // Probe statistics XML definitions

  // XML schema version
  public static final int XML_STAT_MSG_VERSION = 1;

  // XML elements
  public static final String XML_NAS_STAT_ROOT_LABEL = "tw_stats";
  public static final String XML_NAS_STAT_LABEL = "stat";
  public static final String XML_NAS_STAT_GRP_LABEL = "st_grp";

  // element attributes
  public static final String XML_NAS_STAT_VERSION_LABEL = "ver";
  public static final String XML_NAS_STAT_TYPE_LABEL = "type";
  public static final String XML_NAS_STAT_HOST_LABEL = "host";
  public static final String XML_NAS_STAT_NAME_LABEL = "name";
  public static final String XML_NAS_STAT_VALUE_LABEL = "value";

  //////////////////////////////////////////////////////////////////////////////////////
  //
  // Probe statistics grouping types
  public static final String PRB_STATS_GROUP_HOST_TYPE = "Host/Type";
  public static final String PRB_STATS_GROUP_TYPE_HOST = "Type/Host";
}
