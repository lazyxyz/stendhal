package games.stendhal.server.core.rp;

import java.util.Calendar;

/**
 * day light phase
 *
 * @author hendrik
 */
public enum DaylightPhase {

	/** during the night */
	NIGHT (0x47408c),

	/** early morning before sunrise at */
	DAWN (0x774590),

	/** the sun is rising */
	SUNRISE (0xc0a080),

	/** during the day */
	DAY (null),

	/** the sun is setting */
	SUNSET (0xc0a080),

	/** early night */
	DUSK (0x774590);

	private Integer color;

	private DaylightPhase(Integer color) {
		this.color = color;
	}

	/**
	 * gets the current daylight phase
	 *
	 * @return DaylightPhase
	 */
	public static DaylightPhase current() {
		Calendar cal = Calendar.getInstance();

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		// anything but precise, but who cares
		int diffToMidnight = Math.min(hour, 24 - hour);
		if (diffToMidnight > 3) {
			return DAY;
		} else if (diffToMidnight == 3) {
			if (hour < 12) {
				return SUNRISE;
			} else {
				return SUNSET;
			}
		} else if (diffToMidnight == 2) {
			if (hour < 12) {
				return DAWN;
			} else {
				return DUSK;
			}
		} else {
			return NIGHT;
		}
	}

	/**
	 * gets the color of this daylight phase
	 *
	 * @return color
	 */
	public Integer getColor() {
		return color;
	}
}
