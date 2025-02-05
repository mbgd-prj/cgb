package cgdp.component;

import java.awt.event.ActionEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JTextField;

/**
 * ボタンセルエディター。
 *
 */
public abstract class ButtonCellEditor extends DefaultCellEditor {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * コンストラクタ。
	 */
	public ButtonCellEditor() {
		super(new JTextField());
		JButton button = new JButton();
		this.editorComponent = button;
		this.clickCountToStart = 1;
		this.delegate = new EditorDelegate() {

			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void setValue(Object value) {
				((JButton) ButtonCellEditor.this.editorComponent).setText(value.toString());
			}

			@Override
			public Object getCellEditorValue() {
				return ((JButton) ButtonCellEditor.this.editorComponent).getText();
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				ButtonCellEditor.this.action(e);
			}
		};
		button.addActionListener(delegate);
	}

	/**
	 * このメソッドにイベント処理を実装する。
	 * @param e イベント情報。
	 */
	public abstract void action(ActionEvent e);
}
