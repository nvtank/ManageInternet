package Model;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AudioCapture {
	private TargetDataLine line;
	private ByteArrayOutputStream out;
	private Thread recordingThread;
	private volatile boolean isRecording = false;
	
	public void startRecording() throws LineUnavailableException {
		AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		
		line = (TargetDataLine) AudioSystem.getLine(info);
		line.open(format);
		line.start();
		out = new ByteArrayOutputStream();
		isRecording = true;
		
		recordingThread = new Thread(() -> {
			byte[] buffer = new byte[4096];
			while (isRecording) {
				int bytesRead = line.read(buffer, 0, buffer.length);
				out.write(buffer, 0, bytesRead);
			}
		});
		recordingThread.start();
	}
	
	public byte[] stopRecording() throws IOException {
		isRecording = false;
		line.stop();
		line.close();
		try {
			recordingThread.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return out.toByteArray();
	}
	
	public void playAudioFromBytes(byte[] audioData) {
		try {
			AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
			
			ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
			AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
			
			Clip clip = AudioSystem.getClip();
			clip.open(audioInputStream);
			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
