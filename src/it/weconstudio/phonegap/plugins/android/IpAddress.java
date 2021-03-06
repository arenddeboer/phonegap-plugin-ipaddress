package it.weconstudio.phonegap.plugins.android;

import java.io.*;
import java.net.*;
import java.util.*;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

//import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
//import android.widget.TextView;
import android.net.*;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;


import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaResourceApi.OpenForReadResult;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.http.conn.util.InetAddressUtils;

import android.util.Log;

/**
 * IP Address detection plugin
 * 
 * @author weconstudio
 */
public class IpAddress extends CordovaPlugin {
	
	public String   s_dns1 ;
    public String   s_dns2;     
    public String   s_gateway;  
    public String   s_ipAddress;    
    public String   s_leaseDuration;    
    public String   s_netmask;  
    public String   s_serverAddress;

    DhcpInfo d;
    WifiManager wifii;

	@Override
	public boolean execute(String action, JSONArray args,
			final CallbackContext callbackContext) throws JSONException {

		String ipAddress = getIPAddress(true);
		
		wifii= (WifiManager) cordova.getActivity().getSystemService(Context.WIFI_SERVICE);
        d=wifii.getDhcpInfo();
        //http://stackoverflow.com/questions/5387036/programmatically-getting-the-gateway-and-subnet-mask-details
        s_dns1="DNS 1: "+FormatIP(d.dns1);
        s_dns2="DNS 2: "+FormatIP(d.dns2);    
        s_gateway="Default Gateway: "+FormatIP(d.gateway);    
        s_ipAddress="IP Address: "+FormatIP(d.ipAddress); 
        s_leaseDuration="Lease Time: "+String.valueOf(d.leaseDuration);     
        s_netmask="Subnet Mask: "+FormatIP(d.netmask);    
        s_serverAddress="Server IP: "+FormatIP(d.serverAddress);

        //dispaly them
        //info= (TextView) findViewById(R.id.infolbl);
        //info.setText("Network Info\n"+s_dns1+"\n"+s_dns2+"\n"+s_gateway+"\n"+s_ipAddress+"\n"+s_leaseDuration+"\n"+s_netmask+"\n"+s_serverAddress);
		ipAddress = "Network Info\n"+s_dns1+"\n"+s_dns2+"\n"+s_gateway+"\n"+s_ipAddress+"\n"+s_leaseDuration+"\n"+s_netmask+"\n"+s_serverAddress;
		// ipAddress = getIPAddress(true);
		if (ipAddress != null && ipAddress.length() > 0) {
			callbackContext.success(ipAddress);

			return true;
		} else {
			callbackContext.error("Operation failed");

			return false;
		}
	}
	
	public String FormatIP(int IpAddress)
	{
	    return Formatter.formatIpAddress(IpAddress);
	}

	/**
	 * Convert byte array to hex string
	 * 
	 * @param bytes
	 * @return
	 */
	public static String bytesToHex(byte[] bytes) {
		StringBuilder sbuf = new StringBuilder();
		for (int idx = 0; idx < bytes.length; idx++) {
			int intVal = bytes[idx] & 0xff;
			if (intVal < 0x10)
				sbuf.append("0");
			sbuf.append(Integer.toHexString(intVal).toUpperCase());
		}
		return sbuf.toString();
	}

	/**
	 * Get utf8 byte array.
	 * 
	 * @param str
	 * @return array of NULL if error was found
	 */
	public static byte[] getUTF8Bytes(String str) {
		try {
			return str.getBytes("UTF-8");
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Load UTF8withBOM or any ansi text file.
	 * 
	 * @param filename
	 * @return
	 * @throws java.io.IOException
	 */
	public static String loadFileAsString(String filename)
			throws java.io.IOException {
		final int BUFLEN = 1024;
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				filename), BUFLEN);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
			byte[] bytes = new byte[BUFLEN];
			boolean isUTF8 = false;
			int read, count = 0;
			while ((read = is.read(bytes)) != -1) {
				if (count == 0 && bytes[0] == (byte) 0xEF
						&& bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
					isUTF8 = true;
					baos.write(bytes, 3, read - 3); // drop UTF8 bom marker
				} else {
					baos.write(bytes, 0, read);
				}
				count += read;
			}
			return isUTF8 ? new String(baos.toByteArray(), "UTF-8")
					: new String(baos.toByteArray());
		} finally {
			try {
				is.close();
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * Returns MAC address of the given interface name.
	 * 
	 * @param interfaceName
	 *            eth0, wlan0 or NULL=use first interface
	 * @return mac address or empty string
	 */
	public static String getMACAddress(String interfaceName) {
		try {
			List<NetworkInterface> interfaces = Collections
					.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				if (interfaceName != null) {
					if (!intf.getName().equalsIgnoreCase(interfaceName))
						continue;
				}
				byte[] mac = intf.getHardwareAddress();
				if (mac == null)
					return "";
				StringBuilder buf = new StringBuilder();
				for (int idx = 0; idx < mac.length; idx++)
					buf.append(String.format("%02X:", mac[idx]));
				if (buf.length() > 0)
					buf.deleteCharAt(buf.length() - 1);
				return buf.toString();
			}
		} catch (Exception ex) {
		} // for now eat exceptions
		return "";
		/*
		 * try { // this is so Linux hack return
		 * loadFileAsString("/sys/class/net/" +interfaceName +
		 * "/address").toUpperCase().trim(); } catch (IOException ex) { return
		 * null; }
		 */
	}

	/**
	 * Get IP address from first non-localhost interface
	 * 
	 * @param ipv4
	 *            true=return ipv4, false=return ipv6
	 * @return address or empty string
	 */
	public static String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections
					.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf
						.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 port
																// suffix
								return delim < 0 ? sAddr : sAddr.substring(0,
										delim);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
		} // for now eat exceptions
		return "";
	}

}
