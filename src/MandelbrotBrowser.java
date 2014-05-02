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

		JButton b1 = new JButton("Refine");
		b1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				v.refine();
			}});
		buttonPane.add(b1);

		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	public static void main(String [] args)
	{
		MandelbrotBrowser mb = new MandelbrotBrowser();
		mb.setVisible(true);
	}
}
