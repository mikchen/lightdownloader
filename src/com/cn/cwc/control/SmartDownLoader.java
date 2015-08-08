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
 * ���������ļ���ʹ�������ǳ�����
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
	 * @return ���������̵߳�����
	 */
	public DownloadThread[] getDownloadThread(){
		return dt;
	}
	
	/**
	 * 
	 * @param oll
	 *            �����Ҫʵʱ�Ĺ۲��ļ���ȡ�����������д�ýӿ�
	 */
	public void setOnLoadingListener(OnLoadingListener oll) {
		this.oll = oll;
	}

	public boolean isFileExeists() {
		return context.exists();
	}

	/**
	 * @return ���ظ�ָ����ļ���File����
	 */
	public File getContext() {
		return context;
	}

	/**
	 * ���÷�����Ϣ������
	 * 
	 * @param l
	 *            �����Ҫ��Ӧ״̬�룬����������ӿ�
	 */
	public void setOnConnectionListener(OnConnectionListener l) {
		this.l = l;
	}

	/**
	 * 
	 * @param buffLength
	 *            ���û������Ĵ�С��Ĭ��Ϊ1024 Ҳ����1KB
	 *            	���Ϊ10M����СΪ10B
	 */
	public void setBuffLength(int buffLength) {
		if(buffLength<10||buffLength>1024*1024*10)
			return;
		this.buffLength = buffLength;
	}

	/**
	 * 
	 * @param threadCounts
	 *            �����̵߳ĵ�������Ĭ��Ϊ3�����Ϊ10,��СΪ1;
	 */
	public void setThreadCounts(int threadCounts) {
		if(threadCounts>10||threadCounts<1)
			return;
		this.threadCounts = threadCounts;
		status = threadCounts;
	}

	/**
	 * @param url
	 *            �ȴ����ʵ���Դ��·��,����Ϊ��Ŷ,�Լ����Լ�����Ŷ
	 * @param destPath
	 *            �洢�ļ���λ�ã�Ҳ����Ϊ��
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
	 * ����������Դ
	 */
	public void downLoading() {
		new Thread() {

			public void run() {
				URL ur = null;
				try {
					// �õ�URL����
					ur = new URL(url);
					// ������
					HttpURLConnection huc = (HttpURLConnection) ur
							.openConnection();
					// ������������ʽ
					huc.setRequestMethod("GET");
					// ���÷���ʱ��
					huc.setConnectTimeout(5000);
					// �õ���Ӧ��
					int responseCode = huc.getResponseCode();

					// ������ӳɹ�
					if (responseCode == 200) {
						// �õ��ļ��Ĵ�С
						fileLength = huc.getContentLength();
						blockSize = fileLength / threadCounts;
						// ������Ŀ���ļ�ͬ����С���ļ�
						RandomAccessFile raf = new RandomAccessFile(context,
								"rw");
						raf.setLength(fileLength);
						execute();
					} else {
						// �����������Ϊ��
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
	 * ��ͣ���ء�׼ȷ��˵��ȡ�����أ�����֧�ֶϵ����أ������´��ٿ�ʼʱ�����Զ����ϴο�ʼ�ط�����
	 */
	public void cancel() {
		canceled = true;

	}

	// �������߳�����
	protected void execute() {
		// ���������߳�
		for (int id = 0; id < threadCounts; id++) {
			int startByte = id * blockSize;
			int endByte = id == threadCounts - 1 ? fileLength - 1 : (id + 1)
					* blockSize - 1;
			dt[id] = new DownloadThread(id, startByte, endByte);
			dt[id].start();
		}

	}

	/**
	 * ���߳����ص���
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
					//�����ʱ�ļ����ڣ��Ͷ���ʱ�ļ���
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
			// �ж��Ƿ�����ʱ�ļ����ڣ����Ҷ���
			if (currentByte > endByte) {
				if (l != null)
					l.onSuccess(DOWNLOAD_SUCCESS);
				if (oll != null)
					oll.onloading(byteLength, currentByte, id);
				return;
			}
			// ���û�л���û��������
			// �������ļ�
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
		 * ���ڶ��߳������ļ�����֧�ֶϵ�����
		 * 
		 * @throws IOException
		 */
		private void load() throws IOException {
			// ����URL����
			URL uur = new URL(url);
			// ���Դ�����
			HttpURLConnection huuc = (HttpURLConnection) uur.openConnection();
			// ��������ʽ
			huuc.setRequestMethod("GET");
			// ��������ʱ��
			huuc.setConnectTimeout(5000);
			// ��������ͷ��Ϣ����ʾ���ظ��ļ���ĳ������range:bytes=start-end
			huuc.setRequestProperty("range", "bytes=" + currentByte + "-"
					+ endByte);
			// �õ���Ӧ��
			int code = huuc.getResponseCode();
			if (code == 206) {
				// �õ�������
				InputStream in = huuc.getInputStream();
				// �½�һ��RandomAccessFile����������ָ����Χ���ֽ���д����
				RandomAccessFile raff = new RandomAccessFile(context, "rw");
				// ���ø���д�뿪ʼ�ֽ���
				raff.seek(currentByte);
				// io���Կ�
				byte[] bys = new byte[buffLength];
				int len = 0;
				while ((len = in.read(bys)) != -1) {
					raff.write(bys, 0, len);
					// ʵʱ������д�����ֽ���
					currentByte += len;
					// �Ѹ�ֵд�뵽�ļ���,����ʵʱˢ��
					RandomAccessFile tempf = new RandomAccessFile(temp, "rwd");
					tempf.write((currentByte + "").getBytes());
					tempf.close();
					//���ȡ�����أ���ֱ���˳�
					if (canceled)
						return;
					// ����м����������ü�����
					if (oll != null)
						oll.onloading(byteLength, currentByte - startByte, id);
				}
				raff.close();
				in.close();
				// ��ʱ����м������ͷ��ؼ�������Ϣ
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
