package viewer;

import java.io.Serializable;

import javax.swing.SpinnerNumberModel;

@SuppressWarnings("serial")
public class LoopSpinnerModelInteger extends SpinnerNumberModel {
	
	Integer value;
	int max;
	
	public LoopSpinnerModelInteger(int max) {
		this.max = max;
		value = 1;
		fireStateChanged();
	}

	public Object getNextValue() {
		System.out.println("Next value");		
		if (value==max) value = 1;		
		else value++;
		return value;
	}

	public Object getPreviousValue() {
		System.out.println("Previous value");
		if (value==1) value = max;
		else value--;
		return value;
	}

	public Object getValue() {				
		return value;
	}

	public void setValue(Object value) throws IllegalArgumentException {
		if (value==null || ! (value instanceof Integer)) 
			throw new IllegalArgumentException();
		value = (Integer)value;		
		fireStateChanged();
	}

}
