package edu.columbia.cs.psl.phosphor.runtime;

import com.sun.org.apache.xpath.internal.operations.Mult;
import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Logger;
import edu.columbia.cs.psl.phosphor.struct.*;
import sun.text.resources.CollationData;

import java.lang.reflect.Field;

import java.lang.reflect.Array;
import java.util.*;
import java.util.ArrayList;

/**
 * This class handles dynamically doing source-based tainting.
 * 
 * If you want to replace the *value* dynamically, then: 1. extend this class 2.
 * Set Configuration.autoTainter to be an instance of your new
 * TaintSourceWrapper 3. Override autoTaint..., change the value, then call
 * super.autoTaint... in order to set the taint correctly
 * 
 * Example:
 * 
 * 
 * public TaintedIntWithObjTag autoTaint(TaintedIntWithObjTag ret, String
 * source, int argIdx) { ret.val = 100; //Change value to be 100 instead of
 * whatever it was normally ret = super.autoTaint(ret, source, argIdx); //will
 * set the taint return ret; }
 * 
 * @author jon
 *
 */
public class TaintSourceWrapper<T extends AutoTaintLabel> {

	// https://stackoverflow.com/questions/1042798/retrieving-the-inherited-attribute-names-values-using-java-reflection
	private static List<Field> getFields(List<Field> fields, Class<?> type) {
		fields.addAll(Arrays.asList(type.getDeclaredFields()));
		if (type.getSuperclass() != null) {
			getFields(fields, type.getSuperclass());
		}
		return fields;
	}

	public void combineTaintsOnArray(Object inputArray, Taint<T> tag){
		if(tag == null) {
			return;
		}
		if(inputArray instanceof LazyArrayObjTags)
		{
			LazyArrayObjTags array = ((LazyArrayObjTags) inputArray);
			if(array.taints == null)
				array.taints = new Taint[array.getLength()];
			for(int i=0; i < array.getLength();i++)
			{
				if(array.taints[i] == null)
					array.taints[i] = tag.copy();
				else
					array.taints[i].addDependency(tag);
			}

		}else if (inputArray instanceof Object[])
		{
			//Object[]
			for(int i = 0; i < ((Object[]) inputArray).length; i++){
				Object o = ((Object[])inputArray)[i];
				if(o instanceof TaintedWithObjTag)
				{

					Taint existing = (Taint) ((TaintedWithObjTag) o).getPHOSPHOR_TAG();
					if(existing != null)
						existing.addDependency(tag);
					else
						((TaintedWithObjTag) o).setPHOSPHOR_TAG(tag.copy());
				}
			}
		}
	}

	private static void sanitizeTaint(Taint taint) {
		if (taint != null) {
			//Logger.debug("was: " + taint.getTaintLevel());
			taint.setTaintLevel(taint.getTaintLevel().greatestLowerBound(TaintLevel.MAYBE_TAINTED));
		} else {
			//Logger.debug("taint == null");
		}
	}

	/* called by sanitizers */
	public static void sanitize(Object obj) {
		//Logger.info("sanitized: " + obj);
		if(obj instanceof String) {
			Taint[] taints = getStringValueTaints((String) obj);
			if (taints != null) {
				for (Taint t : taints) {
					sanitizeTaint(t);
				}
			}
		} else if(obj instanceof TaintedWithObjTag) {
			sanitizeTaint((Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG());
			if (Configuration.CHECK_OBJECT_FIELDS) {
				for (Field field : getFields(new ArrayList<Field>(), obj.getClass())) {
					try {
						field.setAccessible(true);
						if (field.get(obj) instanceof String) {
							sanitize(field.get(obj));
						}
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}else if(obj instanceof LazyArrayObjTags) {
			LazyArrayObjTags tags = ((LazyArrayObjTags) obj);
			if(tags.taints != null) {
				for(Object i : tags.taints) {
					sanitizeTaint((Taint) i);
				}
			}
		} else if(obj instanceof Object[]) {
			for(Object o : ((Object[]) obj)) {
				sanitize(o);
			}
		} else if(obj instanceof ControlTaintTagStack) {
			ControlTaintTagStack ctrl = (ControlTaintTagStack) obj;
			if (ctrl.taint != null && !ctrl.isEmpty()) {
				sanitize(ctrl.taint);
			}
		} else if(obj instanceof TaintedPrimitiveWithObjTag) {
			sanitizeTaint(((TaintedPrimitiveWithObjTag) obj).taint);
		} else {
			Logger.debug("tainted obj is something else");
		}
	}

	public Taint<AutoTaintLabel> generateTaint(String source) {
		Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
		ArrayList<StackTraceElement> st = new ArrayList<>();
		for (StackTraceElement[] elements : stackTraces.values()) {
			st.addAll(Arrays.asList(elements));
		}

		//StackTraceElement[] st = Thread.currentThread().getStackTrace();
		StackTraceElement[] s = new StackTraceElement[st.size() - 3];
		System.arraycopy(st.toArray(), 3, s, 0, s.length);
		return new Taint<>(new AutoTaintLabel(source, s));
	}

	/* Called by sources for the arguments and return value. */
	@SuppressWarnings("unused")
	public Object autoTaint(Object obj, String baseSource, String actualSource, int argIdx) {
		return autoTaint(obj, generateTaint(baseSource));
	}

	/* Adds the specified tag to the specified object. */
	public Object autoTaint(Object obj, Taint<? extends AutoTaintLabel> tag) {
		tag.setTaintLevel(TaintLevel.TAINTED);
		//Logger.debug("auto tainted: " + obj);
	    if(obj == null) {
	        return null;
        } else if(obj instanceof LazyArrayObjTags) {
			return autoTaint((LazyArrayObjTags) obj, tag);
		} else if(obj instanceof TaintedWithObjTag) {
			return autoTaint((TaintedWithObjTag) obj, tag);
		} else if(obj instanceof TaintedPrimitiveWithObjTag) {
			return autoTaint((TaintedPrimitiveWithObjTag) obj, tag);
		} else if(obj.getClass().isArray()) {
			for(int i = 0; i < Array.getLength(obj); i++) {
				Array.set(obj, i, autoTaint(Array.get(obj, i), tag));
			}
			return obj;
		}
		return obj;
	}

    @SuppressWarnings("unchecked")
	public TaintedWithObjTag autoTaint(TaintedWithObjTag ret, Taint<? extends AutoTaintLabel> tag) {
        Taint prevTag = (Taint)ret.getPHOSPHOR_TAG();
        if(prevTag != null) {
            prevTag.addDependency(tag);
        } else {
            ret.setPHOSPHOR_TAG(tag);
        }
        return ret;
    }

	@SuppressWarnings("unchecked")
	public LazyArrayObjTags autoTaint(LazyArrayObjTags ret, Taint<? extends AutoTaintLabel> tag) {
		Taint[] taintArray = ret.taints;
		if (taintArray != null) {
			for (int i = 0; i < taintArray.length; i++) {
				if(taintArray[i] == null)
					taintArray[i] = tag.copy();
				else
					taintArray[i].addDependency(tag);
			}
		} else {
			ret.setTaints(tag);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public TaintedPrimitiveWithObjTag autoTaint(TaintedPrimitiveWithObjTag ret, Taint<? extends AutoTaintLabel> tag) {
		if (ret.taint != null)
			ret.taint.addDependency(tag);
		else
			ret.taint = tag;
		return ret;
	}

	public static void setStringValueTag(String str, LazyCharArrayObjTags tags) {
		if(str != null) {
			str.valuePHOSPHOR_TAG = tags;
		}
	}

	public static LazyCharArrayObjTags getStringValueTag(String str) {
		if(str == null) {
			return null;
		} else {
			return str.valuePHOSPHOR_TAG;

		}
	}

	public static Taint[] getStringValueTaints(String str) {
		if (getStringValueTag(str) == null) {
			return null;
		} else {
			return getStringValueTag(str).taints;
		}
	}

	/* Called by sink methods. */
	@SuppressWarnings("unused")
	public void checkTaint(Object self, Object[] arguments, String baseSink, String actualSink) {
		if(arguments != null) {
			for(Object argument : arguments) {
				checkTaint(argument, baseSink, actualSink);
			}

			if (self != null && arguments.length == 0) {
				checkTaint(self, baseSink, actualSink);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void checkTaint(Object obj, String baseSink, String actualSink) {
		Logger.debug("checking: " + obj);
		if(obj instanceof String) {
			Taint[] taints = getStringValueTaints((String) obj);
			if (taints != null) {
				SimpleHashSet<String> reported = new SimpleHashSet<>();
				for (Taint t : taints) {
					if (t != null) {
						//System.out.println(t.getTaintLevel());
						String _t = new String(t.toString().getBytes());
						if (reported.add(_t))
							taintViolation(t, obj, baseSink, actualSink);
					}
				}
			}
		} else if(obj instanceof Collection) {
			for (Object o : ((Collection) obj)) {
				checkTaint(o, baseSink, actualSink);
			}
		} else if(obj instanceof Map) {
			for (Object o : (((Map) obj).values())) {
				checkTaint(o, baseSink, actualSink);
			}
		} else if(obj instanceof TaintedWithIntTag) {
			checkTaint(((TaintedWithIntTag)obj).getPHOSPHOR_TAG(), actualSink);
		} else if(obj instanceof TaintedWithObjTag) {
			if(((TaintedWithObjTag) obj).getPHOSPHOR_TAG() != null) {
				taintViolation((Taint<T>) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG(), obj, baseSink, actualSink);
			}
			if (Configuration.CHECK_OBJECT_FIELDS) {
				for (Field field : getFields(new ArrayList<Field>(), obj.getClass())) {
					try {
						field.setAccessible(true);
						//Logger.debug("field name: " + field.getName() + " - of type: " + field.getType().toString());
						if (field.get(obj) instanceof String ||
							field.get(obj) instanceof Map) {
							Logger.debug("field name: " + field.getName());
							checkTaint(field.get(obj), baseSink, actualSink);
						}
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		} else if(obj instanceof LazyArrayIntTags) {
			checkTaints(((LazyArrayIntTags) obj).taints, actualSink);
		} else if(obj instanceof LazyArrayObjTags) {
			LazyArrayObjTags tags = ((LazyArrayObjTags) obj);
			if(tags.taints != null) {
				for(Object i : tags.taints) {
					if(i != null)
						taintViolation((Taint<T>) i, obj, baseSink, actualSink);
				}
			}
		} else if(obj instanceof Object[]) {
			for(Object o : ((Object[]) obj)) {
				checkTaint(o, baseSink, actualSink);
			}
		} else if(obj instanceof ControlTaintTagStack) {
			ControlTaintTagStack ctrl = (ControlTaintTagStack) obj;
			if (ctrl.taint != null && !ctrl.isEmpty()) {
				taintViolation((Taint<T>) ctrl.taint, obj, baseSink, actualSink);
			}
		} else if(obj instanceof TaintedPrimitiveWithObjTag) {
			if(((TaintedPrimitiveWithObjTag)obj).taint != null) {
				taintViolation(((TaintedPrimitiveWithObjTag)obj).taint, ((TaintedPrimitiveWithObjTag)obj).getValue(), baseSink, actualSink);
			}
		} else if(obj instanceof TaintedPrimitiveWithIntTag) {
			checkTaint(((TaintedPrimitiveWithIntTag)obj).taint, actualSink);
		}
	}

    public void taintViolation(Taint<T> tag, Object obj, String baseSink, String actualSink) {
		TaintLevel taintLevel = TaintLevel.fromTaint(tag);
		Logger.debug(actualSink + ", sinking: " + obj + ": " + taintLevel);
		if (taintLevel == TaintLevel.MAYBE_TAINTED) {
			Logger.warning("maybe tainted value sunk!\n" + tag + "\n" + obj);
		} else if (taintLevel == TaintLevel.TAINTED) {
			throw new TaintSinkError(tag, obj);
		}
    }

	public void checkTaint(int tag, String actualSink) {
		if(tag != 0) {
			throw new IllegalAccessError("Argument carries taint " + tag + " at " + actualSink);
		}
	}

	public void checkTaints(int[] tags, String actualSink) {
		if(tags != null) {
			for (int tag : tags) {
				checkTaint(tag, actualSink);
			}
		}
	}

	/* Called just before a sink method returns. */
	public void exitingSink(String sink) {
		return;
	}

	/* Called after a sink method makes its calls to checkTaint but before the rest of the method body executes. */
	public void enteringSink(String sink) {
		return;
	}
}
