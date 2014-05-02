import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.Arrays;
import javax.swing.*;
import org.apfloat.*;

public class MandelbrotView extends JComponent
{
	static final int FSIZE = 128;
	int vscale = 1;
	int offsetX = 0;
	int offsetY = 0;
	int fragmentColumns = 1;
	int fragmentRows = 1;
	FragmentHolder [][] ff;
	Apfloat fsize = new Apfloat(1.0);

	public MandelbrotView()
	{
		ff = new FragmentHolder[fragmentRows][fragmentColumns];
		ff[0][0] = new FragmentHolder(Apcomplex.ZERO, fsize);

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

				aa[i*2+0][j*2+0] = ff[i][j].innerQuadrant(0,0);
				aa[i*2+0][j*2+1] = ff[i][j].innerQuadrant(1,0);
				aa[i*2+1][j*2+0] = ff[i][j].innerQuadrant(0,1);
				aa[i*2+1][j*2+1] = ff[i][j].innerQuadrant(1,1);
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

	static int colorOf(byte b)
	{
		if (b == 0) { return 0; }
		int x = b <= 30 ? 255 - 8*(b-1) : 0;
		return 0xff0000 | (x << 8) | x;
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

				a[i][j] = ff[0][j].neighbor(0,-count+i);
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

				a[i][j] = ff[fragmentRows-1][j].neighbor(0,1+i-fragmentRows);
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
				a[j] = a[fragmentColumns-1].neighbor(1+j-fragmentColumns,0);
			}
			ff[i] = a;
		}
		fragmentColumns += count;
	}

	void expandWest(int count)
	{
		for (int i = 0; i < fragmentRows; i++) {
			FragmentHolder [] a = new FragmentHolder[fragmentColumns+count];
			System.arraycopy(ff[i], 0, a, count, ff[i].length);
			ff[i] = a;

			for (int j = 0; j < count; j++) {
				ff[i][j] = ff[i][j+count].neighbor(-count+j,0);
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

		while (cx/2 + offsetX + FSIZE <= 0) {
			shrinkWest(1);
		}
		while (cx/2 + offsetX > 0) {
			expandWest(1);
		}
		while (cy/2 + offsetY + FSIZE <= 0) {
			shrinkNorth(1);
		}
		while (cy/2 + offsetY > 0) {
			expandNorth(1);
		}

		while (cx/2 + offsetX + FSIZE*fragmentColumns < cx) {
			expandEast(1);
		}
		while (cx/2 + offsetX + FSIZE*fragmentColumns - FSIZE >= cx) {
			shrinkEast(1);
		}

		while (cy/2 + offsetY + FSIZE*fragmentRows < cy) {
			expandSouth(1);
		}
		while (cy/2 + offsetY + FSIZE*fragmentRows - FSIZE >= cy) {
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
		assert f.m.width == FSIZE;
		assert f.m.height == FSIZE;

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

	static enum HolderState
	{
		INIT,
		ZOOM,
		READY;
	}

	static class FragmentGen extends SwingWorker<MandelbrotFragment,Object>
	{
		FragmentHolder h;
		MandelbrotFragment f;

		FragmentGen(FragmentHolder h)
		{
			this.h = h;
		}

		@Override
		public MandelbrotFragment doInBackground()
		{
			f = new MandelbrotFragment(
				h.origin.real(),
				h.origin.imag(),
				h.origin.real().add(h.size),
				h.origin.imag().add(h.size),
				FSIZE, FSIZE);
			return f;
		}

		@Override
		protected void done() {
			h.m = f;
			h.state = HolderState.READY;
			h.fireReady();
		}
	}

	class FragmentHolder
	{
		Apcomplex origin;
		Apfloat size;

		HolderState state = HolderState.INIT;
		MandelbrotFragment m;

		// valid only if state == ZOOM
		FragmentHolder parent;
		int parentX;
		int parentY;

		FragmentHolder(Apcomplex origin, Apfloat size)
		{
			this.origin = origin;
			this.size = size;

			new FragmentGen(this).execute();
		}

		FragmentHolder neighbor(int dx, int dy)
		{
			Apfloat fx = size.multiply(new Apfloat(dx));
			Apfloat fy = size.multiply(new Apfloat(dy));

			return new FragmentHolder(
				new Apcomplex(
					origin.real().add(fx),
					origin.imag().add(fy)
					),
				size);
		}

		FragmentHolder innerQuadrant(int ax, int ay)
		{
			Apfloat hsize = this.size.divide(new Apfloat(2));
			Apfloat re0 = this.origin.real();
			Apfloat im0 = this.origin.imag();

			FragmentHolder h = new FragmentHolder(
				new Apcomplex(
					origin.real().add(hsize.multiply(new Apfloat(ax))),
					origin.imag().add(hsize.multiply(new Apfloat(ay)))
					),
				hsize);
			h.state = HolderState.ZOOM;
			h.setParent(this, ax, ay);
			return h;
		}

		void setParent(FragmentHolder h, int px, int py)
		{
			this.parent = h;
			this.parentX = px;
			this.parentY = py;
		}

		int get(int x, int y)
		{
			switch (state) {
			case READY: return colorOf(m.members[y][x]);
			case ZOOM:
				return parent.get((x+FSIZE*parentX)/2,
					(y+FSIZE*parentY)/2);
			default:
				return 0xeeeeee;
			}
		}

		void fireReady()
		{
			repaint();
		}
	}
}
