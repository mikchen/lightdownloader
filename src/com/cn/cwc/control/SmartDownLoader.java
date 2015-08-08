package com.cn.cwc.control;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import com.cn.cwc.control.dao.OnConnectionListener;
import com.cn.cwc.control.dao.OnLoadingListener;
/** 
 * 用于下载文件，使用起来非常方便
 * @author cwc
 */
public class SmartDownLoader {
	public static final int CONNECTION_FAILED = 400;
	public static final int CONNECTION_ERROR = 500;
	public static final int DOWNLOAD_SUCCESS = 200;
	public static final int DOWNLOAD_FAILED = 100;
	private boolean canceled;
	private String url;
	private String destPath;
	private int threadCounts = 3;
	private int buffLength = 1024;
	private OnConnectionListener l;
	private OnLoadingListener oll;
	private String fileName;
	private int fileLength;
	private File context;
	private static int status = 3;
	private DownloadThread[] dt = new DownloadThread[10];

	
	/**
	 * 
	 * @return 返回所有线程的引用
	 */
	public DownloadThread[] getDownloadThread(){
		return dt;
	}
	
	/**
	 * 
	 * @param oll
	 *            如果需要实时的观测文件读取的情况，就重写该接口
	 */
	public void setOnLoadingListener(OnLoadingListener oll) {
		this.oll = oll;
	}

	public boolean isFileExeists() {
		return context.exists();
	}

	/**
	 * @return 返回该指向该文件的File对象
	 */
	public File getContext() {
		return context;
	}

	/**
	 * 设置返回信息监听器
	 * 
	 * @param l
	 *            如果需要响应状态码，就设置这个接口
	 */
	public void setOnConnectionListener(OnConnectionListener l) {
		this.l = l;
	}

	/**
	 * 
	 * @param buffLength
	 *            设置缓冲区的大小，默认为1024 也就是1KB
	 *            	最大为10M，最小为10B
	 */
	public void setBuffLength(int buffLength) {
		if(buffLength<10||buffLength>1024*1024*10)
			return;
		this.buffLength = buffLength;
	}

	/**
	 * 
	 * @param threadCounts
	 *            设置线程的的数量，默认为3，最大为10,最小为1;
	 */
	public void setThreadCounts(int threadCounts) {
		if(threadCounts>10||threadCounts<1)
			return;
		this.threadCounts = threadCounts;
		status = threadCounts;
	}

	/**
	 * @param url
	 *            等待访问的资源的路径,不能为空哦,自己可以加正则哦
	 * @param destPath
	 *            存储文件的位置，也不能为空
	 */
	public SmartDownLoader(String url, String destPath) {
		this.url = url;
		this.destPath = destPath;
		fileName = url.substring(url.lastIndexOf("/") + 1);
		context = new File(destPath, fileName);
	}

	
	private int blockSize;
	//private Thread t;

	/**
	 * 用于下载资源
	 */
	public void downLoading() {
		new Thread() {

			public void run() {
				URL ur = null;
				try {
					// 拿到URL对象
					ur = new URL(url);
					// 打开连接
					HttpURLConnection huc = (HttpURLConnection) ur
							.openConnection();
					// 设置设置请求方式
					huc.setRequestMethod("GET");
					// 设置访问时限
					huc.setConnectTimeout(5000);
					// 拿到响应码
					int responseCode = huc.getResponseCode();

					// 如果连接成功
					if (responseCode == 200) {
						// 拿到文件的大小
						fileLength = huc.getContentLength();
						blockSize = fileLength / threadCounts;
						// 创建和目标文件同样大小的文件
						RandomAccessFile raf = new RandomAccessFile(context,
								"rw");
						raf.setLength(fileLength);
						execute();
					} else {
						// 如果监听器不为空
						if (l != null)
							l.onError(CONNECTION_FAILED);

					}

				} catch (IOException e) {
					if (l != null)
						l.onError(CONNECTION_ERROR);
					e.printStackTrace();
				}

			};
		}.start();

	}

	/**
	 * 暂停下载。准确来说是取消下载，由于支持断点下载，所以下次再开始时，会自动从上次开始地方下载
	 */
	public void cancel() {
		canceled = true;

	}

	// 开启多线程下载
	protected void execute() {
		// 开启下载线程
		for (int id = 0; id < threadCounts; id++) {
			int startByte = id * blockSize;
			int endByte = id == threadCounts - 1 ? fileLength - 1 : (id + 1)
					* blockSize - 1;
			dt[id] = new DownloadThread(id, startByte, endByte);
			dt[id].start();
		}

	}

	/**
	 * 多线程下载的类
	 * 
	 * @author cwc
	 * 
	 */
	public class DownloadThread extends Thread {
		private static final int DOWNLOAD_ERROR = 0;
		private int id;
		private int startByte;
		private int endByte;
		private File temp;
		private int currentByte;
		private int byteLength;

		public DownloadThread(int id, int startByte, int endByte) {

			this.id = id;
			this.startByte = startByte;
			this.endByte = endByte;
			byteLength = endByte - startByte;
			temp = new File(destPath, id + ".temp");
			if (temp.exists()) {
				try {
					//如果临时文件存在，就读零时文件。
					BufferedReader br = new BufferedReader(new FileReader(temp));
					currentByte = Integer.parseInt(br.readLine());
					br.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			} else {
				currentByte = startByte;
			}
		}

		@Override
		public void run() {
			// 判断是否有临时文件存在，并且读完
			if (currentByte > endByte) {
				if (l != null)
					l.onSuccess(DOWNLOAD_SUCCESS);
				if (oll != null)
					oll.onloading(byteLength, currentByte, id);
				return;
			}
			// 如果没有或者没有下载完
			// 就下载文件
			try {
				load();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				if (l != null)
					l.onError(DOWNLOAD_ERROR);
			}

		}

		/**
		 * 用于多线程下载文件，可支持断点下载
		 * 
		 * @throws IOException
		 */
		private void load() throws IOException {
			// 创建URL对象
			URL uur = new URL(url);
			// 尝试打开连接
			HttpURLConnection huuc = (HttpURLConnection) uur.openConnection();
			// 设置请求方式
			huuc.setRequestMethod("GET");
			// 设置请求时限
			huuc.setConnectTimeout(5000);
			// 设置请求头信息，表示下载该文件的某个部分range:bytes=start-end
			huuc.setRequestProperty("range", "bytes=" + currentByte + "-"
					+ endByte);
			// 拿到响应码
			int code = huuc.getResponseCode();
			if (code == 206) {
				// 拿到输入流
				InputStream in = huuc.getInputStream();
				// 新建一个RandomAccessFile对象，用于往指定范围的字节里写数据
				RandomAccessFile raff = new RandomAccessFile(context, "rw");
				// 设置该流写入开始字节数
				raff.seek(currentByte);
				// io流对考
				byte[] bys = new byte[buffLength];
				int len = 0;
				while ((len = in.read(bys)) != -1) {
					raff.write(bys, 0, len);
					// 实时更新所写到的字节数
					currentByte += len;
					// 把该值写入到文件中,并且实时刷新
					RandomAccessFile tempf = new RandomAccessFile(temp, "rwd");
					tempf.write((currentByte + "").getBytes());
					tempf.close();
					//如果取消下载，就直接退出
					if (canceled)
						return;
					// 如果有监听器就设置监听器
					if (oll != null)
						oll.onloading(byteLength, currentByte - startByte, id);
				}
				raff.close();
				in.close();
				// 此时如果有监听器就返回监听器信息
				synchronized (SmartDownLoader.class) {
					status--;
					if (status <= 0) {
						for (int i = 0; i < threadCounts; i++) {
							new File(destPath, i + ".temp").delete();
						}
						if (l != null)
							l.onSuccess(DOWNLOAD_SUCCESS);
					}
				}
			} else {
				if (l != null)
					l.onFailed(DOWNLOAD_FAILED);
			}
		}
	}
}
