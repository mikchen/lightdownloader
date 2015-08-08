package com.cn.cwc.control.dao;

public interface OnConnectionListener {
	/**
	 * 
	 * @param 返回下载成功的状态码 这些状态码  都是我随便写的，如果有需要可以自定义值 
	 */
	public void onSuccess(int responseCode);
	/**
	 * 
	 * @param 
	 */
	public void onFailed(int responseCode);
	/**
	 * 
	 * @param
	 */
	public void onError(int responseCode);
}
