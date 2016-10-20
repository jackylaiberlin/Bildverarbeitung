
// BV Ue1 WS2016/17 Vorgabe
//
// Copyright (C) 2015 by Klaus Jung

import javax.swing.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.awt.Color;

public class NichtlinFilter extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final String author = "<LaiPaetzold>"; // TODO: type in your name
														// here
	private static final String initialFilename = "lena_klein.png";
	private static final File openPath = new File(".");
	private static final int borderWidth = 5;
	private static final int maxWidth = 446;
	private static final int maxHeight = maxWidth;
	private static final int maxNoise = 30; // in per cent

	private static JFrame frame;

	private ImageView srcView; // source image view
	private ImageView dstView; // filtered image view

	private int[] origPixels = null;

	private JLabel statusLine = new JLabel("     "); // to print some status
														// text

	private JComboBox<String> noiseType;
	private JLabel noiseLabel;
	private JSlider noiseSlider;
	private JLabel noiseAmountLabel;
	private boolean addNoise = false;
	private double noiseFraction = 0.01; // fraction for number of pixels to be
											// modified by noise

	private JComboBox<String> filterType;

	public NichtlinFilter() {
		super(new BorderLayout(borderWidth, borderWidth));

		setBorder(BorderFactory.createEmptyBorder(borderWidth, borderWidth, borderWidth, borderWidth));

		// load the default image
		File input = new File(initialFilename);

		if (!input.canRead())
			input = openFile(); // file not found, choose another image

		srcView = new ImageView(input);
		srcView.setMaxSize(new Dimension(maxWidth, maxHeight));

		// convert to grayscale
		makeGray(srcView);

		// keep a copy of the grayscaled original image pixels
		origPixels = srcView.getPixels().clone();

		// create empty destination image of same size
		dstView = new ImageView(srcView.getImgWidth(), srcView.getImgHeight());
		dstView.setMaxSize(new Dimension(maxWidth, maxHeight));

		// control panel
		JPanel controls = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, borderWidth, 0, 0);

		// load image button
		JButton load = new JButton("Open Image");
		load.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadFile(openFile());
				// convert to grayscale
				makeGray(srcView);
				// keep a copy of the grayscaled original image pixels
				origPixels = srcView.getPixels().clone();
				calculate(true);
			}
		});

		// selector for the noise method
		String[] noiseNames = { "No Noise ", "Salt & Pepper " };

		noiseType = new JComboBox<String>(noiseNames);
		noiseType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addNoise = noiseType.getSelectedIndex() > 0;
				noiseLabel.setEnabled(addNoise);
				noiseSlider.setEnabled(addNoise);
				noiseAmountLabel.setEnabled(addNoise);
				calculate(true);
			}
		});

		// amount of noise
		noiseLabel = new JLabel("Noise:");
		noiseAmountLabel = new JLabel("" + Math.round(noiseFraction * 100.0) + " %");
		noiseSlider = new JSlider(JSlider.HORIZONTAL, 0, maxNoise, (int) Math.round(noiseFraction * 100.0));
		noiseSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				noiseFraction = noiseSlider.getValue() / 100.0;
				noiseAmountLabel.setText("" + Math.round(noiseFraction * 100.0) + " %");
				calculate(true);
			}
		});
		noiseLabel.setEnabled(addNoise);
		noiseSlider.setEnabled(addNoise);
		noiseAmountLabel.setEnabled(addNoise);

		// selector for filter
		String[] filterNames = { "No Filter", "Min Filter", "Max Filter", "Box Filter", "Median Filter" };
		filterType = new JComboBox<String>(filterNames);
		filterType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				calculate(false);
			}
		});

		controls.add(load, c);
		controls.add(noiseType, c);
		controls.add(noiseLabel, c);
		controls.add(noiseSlider, c);
		controls.add(noiseAmountLabel, c);
		controls.add(filterType, c);

		// images panel
		JPanel images = new JPanel(new GridLayout(1, 2));
		images.add(srcView);
		images.add(dstView);

		// status panel
		JPanel status = new JPanel(new GridBagLayout());

		status.add(statusLine, c);

		add(controls, BorderLayout.NORTH);
		add(images, BorderLayout.CENTER);
		add(status, BorderLayout.SOUTH);

		calculate(true);

	}

	private File openFile() {
		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Images (*.jpg, *.png, *.gif)", "jpg", "png",
				"gif");
		chooser.setFileFilter(filter);
		chooser.setCurrentDirectory(openPath);
		int ret = chooser.showOpenDialog(this);
		if (ret == JFileChooser.APPROVE_OPTION)
			return chooser.getSelectedFile();
		return null;
	}

	private void loadFile(File file) {
		if (file != null) {
			srcView.loadImage(file);
			srcView.setMaxSize(new Dimension(maxWidth, maxHeight));
			// create empty destination image of same size
			dstView.resetToSize(srcView.getImgWidth(), srcView.getImgHeight());
			frame.pack();
		}

	}

	private static void createAndShowGUI() {
		// create and setup the window
		frame = new JFrame("Nonlinear Filters - " + author);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JComponent newContentPane = new NichtlinFilter();
		newContentPane.setOpaque(true); // content panes must be opaque
		frame.setContentPane(newContentPane);

		// display the window.
		frame.pack();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		frame.setLocation((screenSize.width - frame.getWidth()) / 2, (screenSize.height - frame.getHeight()) / 2);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	private void calculate(boolean createNoise) {
		long startTime = System.currentTimeMillis();

		if (createNoise) {
			// start with original image pixels
			srcView.setPixels(origPixels);
			// add noise
			if (addNoise)
				makeNoise(srcView);
			// make changes visible
			srcView.applyChanges();
		}

		// apply filter
		filter();

		// make changes visible
		dstView.applyChanges();

		long time = System.currentTimeMillis() - startTime;
		statusLine.setText("Processing Time = " + time + " ms");
	}

	private void makeGray(ImageView imgView) {
		int pixels[] = imgView.getPixels();
		// TODO: convert pixels to grayscale

		// loop over all pixels
		for (int i = 0; i < pixels.length; i++) {

			int r = (pixels[i] >> 16) & 0xff;
			int g = (pixels[i] >> 8) & 0xff;
			int b = pixels[i] & 0xff;

			// Formel zur Umrechnung in Graustufenbild
			int argb = (r + g + b) / 3;

			// Shiften des neuen Werts im Pixels-Array
			pixels[i] = (0xFF << 24) | (argb << 16) | (argb << 8) | argb;
		}
	}


	private void makeNoise(ImageView imgView) {
		int pixels[] = imgView.getPixels();

		// TODO: add noise to pixels
		double temp, temp2;

		for (int i = 0; i < pixels.length; i++) {
			
			// Variabel temp random Werte zuweisen und mit noiseFraction vergleichen
			if ((temp = Math.random()) <= noiseFraction) {
				int r = (pixels[i] >> 16) & 0xff;
				int g = (pixels[i] >> 8) & 0xff;
				int b = pixels[i] & 0xff;

				if ((temp2 = Math.random()) > 0.5) {
					r = g = b = 0;
				} else {
					r = g = b = 255;
				}
				pixels[i] = (0xFF << 24) | (r << 16) | (g << 8) | b;

			}
		}

		// for(int i = 0; i<pixels.length; i++){
		// if((temp = Math.random()) <= noiseFraction){
		// pixels[i] = 0;
		// } else if((temp = Math.random()) <= high){
		// pixels[i] = 255;
		// }
		// }
	}

	private int[] getNeighbours(int x, int y, int[] orgPixels, int imwidth, int imheight) {
		int[] neighbours = new int[9];

		int i = 0;
		for (int yOff = -1; yOff <= 1; yOff++) {
			for (int xOff = -1; xOff <= 1; xOff++) {
				if (x == (imwidth - 1) || x == 0 || y == (imheight - 1) || y == 0) {

					neighbours[i] = orgPixels[x + imwidth * y];
				} else {
					neighbours[i] = orgPixels[x + xOff + imwidth * (y + yOff)];
				}

				i++;
			}
		}

		return neighbours;
	}

	private void filter() {
		int src[] = srcView.getPixels();
		int dst[] = dstView.getPixels();
		int width = srcView.getImgWidth();
		int height = srcView.getImgHeight();
		int filterIndex = filterType.getSelectedIndex();

		// TODO: implement filters

		// Minimumsfilter
		if (filterIndex == 1) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int pos = y * width + x;
					int newR, newG, newB;
					newR = newG = newB = 0;

					int[] neighbours = getNeighbours(x, y, src, width, height);
					
					//Zuweisung des ersten Wertes im Array
					int min = neighbours[0];
					
					//Loop durch den Array
					for (int i = 0; i < neighbours.length; i++) {
						int r = (neighbours[i] >> 16) & 0xff;
						int g = (neighbours[i] >> 8) & 0xff;
						int b = (neighbours[i]) & 0xff;

						//Wenn aktueller Wert kleiner als Minimum ist
						// Minimum = i
						if (neighbours[i] < min) {
							min = neighbours[i];
						}
						
						//Minimum wird neuen RGB Werten zugewiesen
						newR = (min >> 16) & 0xff;
						newG = (min >> 8) & 0xff;
						newB = min & 0xff;

						dst[pos] = (0xFF << 24) | (newR << 16) | (newG << 8) | newB;
					}

				}
			}
		}

		// Maximumsfilter
		if (filterIndex == 2) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int pos = y * width + x;
					int newR, newG, newB;
					newR = newG = newB = 0;

					int[] neighbours = getNeighbours(x, y, src, width, height);

					int max = neighbours[0];
					for (int i = 0; i < neighbours.length; i++) {
						int r = (neighbours[i] >> 16) & 0xff;
						int g = (neighbours[i] >> 8) & 0xff;
						int b = (neighbours[i]) & 0xff;

						if (neighbours[i] > max) {
							max = neighbours[i];
						}
						newR = (max >> 16) & 0xff;
						newG = (max >> 8) & 0xff;
						newB = max & 0xff;

						dst[pos] = (0xFF << 24) | (newR << 16) | (newG << 8) | newB;
					}

				}
			}
		}

			// Box-Filter
			if (filterIndex == 3) {
				// 3x3 Filter-Matrix
				int[] filterMatrix = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 };
				int scale = 9;
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						// Aktuelle Position im Bild
						int pos = y * width + x;
						int[] neighbours = getNeighbours(x, y, src, width, height);

						int newR = 0, newG = 0, newB = 0;

						for (int i = 0; i < neighbours.length; i++) {
							int r = (neighbours[i] >> 16) & 0xff;
							int g = (neighbours[i] >> 8) & 0xff;
							int b = neighbours[i] & 0xff;

							// Anwendung des Filters auf RGB-Werte
							newR += r * filterMatrix[i];
							newG += g * filterMatrix[i];
							newB += b * filterMatrix[i];
						}

						// Anwendung des Scales auf neue RGB-Werte
						newR /= scale;
						newG /= scale;
						newB /= scale;

						dst[pos] = (0xFF << 24) | (newR << 16) | (newG << 8) | newB;
					}
				}
			}

			// Medianfilter
			if (filterIndex == 4) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int pos = y * width  + x;
						
						int newR, newG, newB, median;
						newR = newG = newB = 0;
						
						int neighbours[] = getNeighbours(x, y, src, width, height);
						
						// Sortierung des Neighbours-Arrays
						Arrays.sort(neighbours);
						
						// Bestimmung des Medians vom Arrays
						int middle = ((neighbours.length)/2);

						// wenn der Median "zwischen" zwei Werten liegt
						if ((neighbours.length % 2) == 0) {
							int median1 = neighbours[middle];
							int median2 = neighbours[middle-1];
							median = (median1 + median2) / 2;
						}
						else {
							median = neighbours[middle-1];
							}
						
						// Zuweisung des Medians zu den neuen RGB-Werten
						newR = (median >> 16) & 0xFF;
						newG = (median >> 8) & 0xFF;
						newB = (median) & 0xFF;
						dst[pos] = (0xFF << 24) | (newR << 16) | (newG << 8) | newB;
					}
				}
			}
		
	}
}
