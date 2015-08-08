package com.cn.cwc.control.dao;

public interface OnLoadingListener {
	/**
	 * 
	 * @param byteLength 当前要下载文件的总长
	 * @param currentByte 当前下载了多少
	 * @param threadId 线程的ID
	 */
	public void onloading(int byteLength,int currentByte,int threadId);
}
