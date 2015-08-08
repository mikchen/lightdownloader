package com.cn.cwc.control;

import com.cn.cwc.control.dao.OnLoadingListener;

public class Test {

	public static void main(String[] args) {
		String path = "http://192.168.1.103:8080/cwc.exe";
		String destPath = "F:\\diany";
		SmartDownLoader sdl = new SmartDownLoader(path, destPath);
		sdl.setBuffLength(1024*8);
		sdl.setOnLoadingListener(new OnLoadingListener() {
			
			@Override
			public void onloading(int byteLength, int currentByte, int threadId) {
				System.out.println("id="+threadId+"读取了"+currentByte+"总计"+byteLength);
			}
		});
		sdl.downLoading();
	}

}
