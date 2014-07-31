package nav.script.gui;

import java.awt.BorderLayout;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

class CssTextPaneTest {
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		final CssTextPane pane = new CssTextPane();

		pane.defineStyle("*        { color: #000; font-style: normal; font-size:12; font-family: courier; } ");
		pane.defineStyle("digit    { color: #00f; font-style: normal; } ");
		pane.defineStyle("address  { color: #c0c; font-style: normal; } ");
		pane.defineStyle("const    { color: #080; font-style: normal; } ");
		pane.defineStyle("labeluse { color: #c40; font-style: normal; } ");
		pane.defineStyle("labeldef { color: #808; font-style: normal; } ");
		pane.defineStyle("comment  { color: #088; font-style: italic; text-decoration: none } ");

		String pre = "^|\\s|\\G";
		String post = "\\s|//|$";

		{
			pane.addSyntaxElement("digit", Pattern.compile("(" + pre + ")(\\d+)(" + post + ")"), 2);
			pane.addSyntaxElement("labeluse", Pattern.compile("(" + pre + ")(RAISE)(" + post + ")"), 2);
			pane.addSyntaxElement("address", Pattern.compile("(" + pre + ")(GOTO|CALL|HALT|BREAK|REPEAT|SLEEP|YIELD)(" + post + ")"), 2);
			pane.addSyntaxElement("const", Pattern.compile("(" + pre + ")(BEGIN|END|FUNCTION|IF|ELSE|TRAP)(" + post + ")"), 2);
			pane.addSyntaxElement("comment", Pattern.compile("(#.*$)"), 1);
		}

		pane.activateUndoRedo();
		pane.activateAutoRestyle();

		JFrame frame = new JFrame("CSS");
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(pane.wrapInScrollPane(640, 480));
		frame.setResizable(true);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}