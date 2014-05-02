import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.Arrays;
import javax.swing.*;
import org.apfloat.*;

public class MandelbrotView extends JComponent
	implements FragmentHolder.Listener
{
	static final int FSIZE = FragmentHolder.FSIZE;

	int vscale = 1;
	int offsetX = 0;
	int offsetY = 0;
	int fragmentColumns = 1;
	int fragmentRows = 1;
	FragmentHolder [][] ff;
	Apfloat fsize = new Apfloat(1.0);

	public MandelbrotView()
	{
		reset();

		MouseAdapter mouse = new MouseAdapter() {
		public void mousePressed(MouseEvent evt) {
			onMousePressed(evt);
		}
		public void mouseDragged(MouseEvent evt) {
			onMouseDragged(evt);
		}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	Point dragPt;

	void onMousePressed(MouseEvent evt)
	{
		dragPt = evt.getPoint();
	}

	void onMouseDragged(MouseEvent evt)
	{
		if (dragPt == null) { return; }

		int dx = evt.getX() - dragPt.x;
		int dy = evt.getY() - dragPt.y;

		offsetX += dx;
		offsetY += dy;
		dragPt = evt.getPoint();
		repaint();
	}

	public void reset()
	{
		ff = new FragmentHolder[fragmentRows][fragmentColumns];
		ff[0][0] = FragmentHolder.rootFragment();
		offsetX = -FSIZE/2;
		offsetY = -FSIZE/2;
		repaint();
	}

	public void refine()
	{
		zoomIn();
	}

	public void zoomIn()
	{
		FragmentHolder[][] aa = new FragmentHolder[fragmentRows*2][fragmentColumns*2];
		for (int i = 0; i < fragmentRows; i++) {
			for (int j = 0; j < fragmentColumns; j++) {

				Apfloat re0 = ff[i][j].origin.real();
				Apfloat im0 = ff[i][j].origin.imag();

				aa[i*2+0][j*2+0] = ff[i][j].getChild(0,0);
				aa[i*2+0][j*2+0].addListener(this);
				aa[i*2+0][j*2+1] = ff[i][j].getChild(1,0);
				aa[i*2+0][j*2+1].addListener(this);
				aa[i*2+1][j*2+0] = ff[i][j].getChild(0,1);
				aa[i*2+1][j*2+0].addListener(this);
				aa[i*2+1][j*2+1] = ff[i][j].getChild(1,1);
				aa[i*2+1][j*2+1].addListener(this);
			}
		}

		fragmentRows *= 2;
		fragmentColumns *= 2;
		ff = aa;
		fsize = ff[0][0].size;
		offsetX *= 2;
		offsetY *= 2;

		repaint();
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(640,480);
	}

	void shrinkNorth(int count)
	{
		assert count < fragmentRows;

		ff = Arrays.copyOfRange(ff, count, ff.length);
		offsetY += count*FSIZE;
		fragmentRows -= count;
	}

	void shrinkSouth(int count)
	{
		assert count < fragmentRows;

		ff = Arrays.copyOfRange(ff, 0, fragmentRows-count);
		fragmentRows -= count;
	}

	void shrinkEast(int count)
	{
		assert count < fragmentColumns;

		for (int i = 0; i < fragmentRows; i++) {
			ff[i] = Arrays.copyOfRange(ff[i], 0, fragmentColumns-count);
		}
		fragmentColumns -= count;
	}

	void shrinkWest(int count)
	{
		assert count < fragmentColumns;

		for (int i = 0; i < fragmentRows; i++) {
			ff[i] = Arrays.copyOfRange(ff[i], count, ff[i].length);
		}
		offsetX += count*FSIZE;
		fragmentColumns -= count;
	}

	void expandNorth(int count)
	{
		FragmentHolder [][] a = new FragmentHolder[fragmentRows+count][];

		for (int i = 0; i < count; i++) {
			a[i] = new FragmentHolder[fragmentColumns];
			for (int j = 0; j < fragmentColumns; j++) {

				a[i][j] = subscribe(ff[0][j].getNeighbor(0,-count+i));
			}
		}
		System.arraycopy(ff, 0, a, count, fragmentRows);
		ff = a;

		offsetY -= count*FSIZE;
		fragmentRows += count;
	}

	void expandSouth(int count)
	{
		FragmentHolder [][] a = new FragmentHolder[fragmentRows+count][];

		System.arraycopy(ff, 0, a, 0, fragmentRows);
		for (int i = fragmentRows; i < a.length; i++) {
			a[i] = new FragmentHolder[fragmentColumns];
			for (int j = 0; j < fragmentColumns; j++) {

				a[i][j] = subscribe(ff[fragmentRows-1][j].getNeighbor(0,1+i-fragmentRows));
			}
		}
		ff = a;

		fragmentRows += count;
	}

	void expandEast(int count)
	{
		for (int i = 0; i < fragmentRows; i++) {
			FragmentHolder [] a = new FragmentHolder[fragmentColumns+count];
			System.arraycopy(ff[i], 0, a, 0, ff[i].length);

			for (int j = fragmentColumns; j < a.length; j++) {
				a[j] = subscribe(a[fragmentColumns-1].getNeighbor(1+j-fragmentColumns,0));
			}
			ff[i] = a;
		}
		fragmentColumns += count;
	}

	FragmentHolder subscribe(FragmentHolder f)
	{
		if (f != null) {
			f.addListener(this);
		}
		return f;
	}

	boolean canExpandWest()
	{
		return ff[0][0].hasNeighbor(-1,0);
	}

	boolean canExpandNorth()
	{
		return ff[0][0].hasNeighbor(0,-1);
	}

	boolean canExpandSouth()
	{
		return ff[ff.length-1][0].hasNeighbor(0,1);
	}

	boolean canExpandEast()
	{
		return ff[0][fragmentColumns-1].hasNeighbor(1,0);
	}

	void expandWest(int count)
	{
		for (int i = 0; i < fragmentRows; i++) {
			FragmentHolder [] a = new FragmentHolder[fragmentColumns+count];
			System.arraycopy(ff[i], 0, a, count, ff[i].length);
			ff[i] = a;

			for (int j = 0; j < count; j++) {
				ff[i][j] = subscribe(ff[i][j+count].getNeighbor(-count+j,0));
			}
		}
		offsetX -= count*FSIZE;
		fragmentColumns += count;
	}

	@Override
	public void paintComponent(Graphics gr)
	{
		final Insets INSETS = getInsets();
		int cx = getWidth() - (INSETS.left + INSETS.right);
		int cy = getHeight() - (INSETS.top + INSETS.bottom);

		gr.setColor(Color.WHITE);
		gr.fillRect(INSETS.left, INSETS.top, cx, cy);

		while (cx/2 + offsetX + FSIZE <= 0 && fragmentColumns >= 2) {
			shrinkWest(1);
		}
		while (cx/2 + offsetX > 0 && canExpandWest()) {
			expandWest(1);
		}
		while (cy/2 + offsetY + FSIZE <= 0 && fragmentRows >= 2) {
			shrinkNorth(1);
		}
		while (cy/2 + offsetY > 0 && canExpandNorth()) {
			expandNorth(1);
		}

		while (cx/2 + offsetX + FSIZE*fragmentColumns < cx && canExpandEast()) {
			expandEast(1);
		}
		while (cx/2 + offsetX + FSIZE*fragmentColumns - FSIZE >= cx && fragmentColumns >= 2) {
			shrinkEast(1);
		}

		while (cy/2 + offsetY + FSIZE*fragmentRows < cy && canExpandSouth()) {
			expandSouth(1);
		}
		while (cy/2 + offsetY + FSIZE*fragmentRows - FSIZE >= cy && fragmentRows >= 2) {
			shrinkSouth(1);
		}

		for (int i = 0; i < ff.length; i++) {
			for (int j = 0; j < ff[i].length; j++) {

				drawFragment(gr, ff[i][j],
					INSETS.left + cx/2 + offsetX + FSIZE*vscale*j,
					INSETS.top + cy/2 + offsetY + FSIZE*vscale*i
					);
			}
		}
	}

	void drawFragment(Graphics gr, FragmentHolder f, int x, int y)
	{
		if (f == null) {
			gr.setColor(new Color(0xee,0xee,0xff));
			gr.fillRect(x, y, FSIZE*vscale, FSIZE*vscale);
			return;
		}

		assert f != null;

		BufferedImage img = new BufferedImage(FSIZE, FSIZE, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < FSIZE; i++) {
			for (int j = 0; j < FSIZE; j++) {
				img.setRGB(j,i,f.get(j,i));
			}
		}

		gr.drawImage(img,
			x,
			y,
			FSIZE*vscale,
			FSIZE*vscale,
			null);

		gr.setColor(Color.BLUE);
		gr.drawRect(x, y, FSIZE*vscale, FSIZE*vscale);
	}

	public void fragmentReady(FragmentHolder f)
	{
		repaint();
	}
}
