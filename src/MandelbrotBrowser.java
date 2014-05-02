import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MandelbrotBrowser extends JFrame
{
	MandelbrotView v;

	public MandelbrotBrowser()
	{
		v = new MandelbrotView();
		getContentPane().add(v, BorderLayout.CENTER);

		JPanel buttonPane = new JPanel();
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		JButton b1 = new JButton("Zoom In");
		b1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				v.zoomIn();
				updateTitle();
			}});
		buttonPane.add(b1);

		JButton b3 = new JButton("Zoom Out");
		b3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				v.zoomOut();
				updateTitle();
			}});
		buttonPane.add(b3);

		JButton b2 = new JButton("Reset");
		b2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				v.reset();
			}});
		buttonPane.add(b2);

		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	void updateTitle()
	{
		setTitle(String.format("Depth %d",
			v.ff[0][0].address.depth
			));
	}

	public static void main(String [] args)
	{
		MandelbrotBrowser mb = new MandelbrotBrowser();
		mb.setVisible(true);
	}
}
