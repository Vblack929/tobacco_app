package com.bland;


import java.io.*;


/**
 * APP首先调用 IDReader.ServiceStart 并传入配置文件目录，实现启动WebAPI服务和联网读卡服务。
 * 最后APP使用 WebSocketAPI 接口实现读取身份证。
 * @author ZengDingGuo
 * @version 1.2
 */
public class IDReader {

	private synchronized static String loadLib(String configDir, String libName) throws IOException {   
		String systemType = System.getProperty("os.name");   
		String libExtension = "";   
		String libFullName;

		if(systemType.toLowerCase().indexOf("win")!=-1)  {
			libExtension = ".dll";
			libFullName = libName + libExtension;   
		}
		else {
			libExtension = ".so";
			libFullName = "lib" + libName + libExtension;
		}
		
		// Load directly from the config directory where we have the DLL files
		File libFile = new File(configDir + File.separator + libFullName);   
		if(!libFile.exists()){   
			throw new IOException("Library file not found: " + libFile.getAbsolutePath());
		}
		
		System.load(libFile.getAbsolutePath()); 
        // System.out.println("Loaded library: " + libFile.getAbsolutePath()); 
		return libFile.getAbsolutePath();
	}

    /**
     * 启动 WebAPI 服务以及阅读器联网服务
     * @param CfgPath 调用者提供保存配置文件的路径，必须提供且确保正确。如果配置文件不存在，会自动生成默认配置。
     * @return 返回值小于0为失败.
     */
    static public native int ServiceStart(String CfgPath);

    /**
     * 停止 WebAPI 服务以及阅读器联网服务，
     * 执行需要一定时间，会产生阻塞。
     * @return 返回值小于0为失败.
     */
    static public native int ServiceStop();

	static public int Init(String CfgPath)
	{
		//String nativeTempDir = System.getProperty("java.io.tmpdir");   
		int ret;

		try
		{
			String systemType = System.getProperty("os.name");   
			String systemArch = System.getProperty("os.arch");   
			
			// Check if running on Windows
			if(systemType.toLowerCase().indexOf("win")!=-1) {
				// Windows platform - load native libraries
				if(systemArch.toLowerCase().indexOf("64")!=-1) {
					loadLib(CfgPath,"CH9326DLL64");
				}
				else {
					loadLib(CfgPath,"CH9326DLL");
				}
				loadLib(CfgPath,"event");
				loadLib(CfgPath,"event_core");
				loadLib(CfgPath,"event_extra");
				loadLib(CfgPath,"http_client");
				loadLib(CfgPath,"win_hid_dev");
				loadLib(CfgPath,"idreader");

				ret = ServiceStart(CfgPath);
			}
			else {
				// Non-Windows platform - ID card reader not supported
				// System.out.println("警告: 身份证读卡器仅支持Windows平台，当前平台: " + systemType + " " + systemArch);
				ret = -1; // Return failure code for non-Windows platforms
			}
		}
		catch (Exception e)
		{
			// System.out.println("IDReader初始化失败: " + e.getMessage());
			e.printStackTrace(); 
			ret = -1;
		}
		return ret;
	}

    /**
     * 直接调用 WebAPI 服务提供的 websocket 接口
     * @param cmd json 格式的指令。
     * @return 返回指令执行结果.
     */
    public String WebSocketAPI(String cmd) {
        String systemType = System.getProperty("os.name");
        if(systemType.toLowerCase().indexOf("win") == -1) {
            // Non-Windows platform - return error response
            return "{\"errorMsg\":\"身份证读卡器仅支持Windows平台\",\"ret\":-1}";
        }
        // On Windows, this should call the native method
        return WebSocketAPINative(cmd);
    }
    
    /**
     * Native implementation for Windows
     */
    private native String WebSocketAPINative(String cmd);
}
