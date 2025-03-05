package BoardGame.constant;

public enum UserTier {
	BRONZE5(0, 199),
  BRONZE4(200, 399),
  BRONZE3(400, 599),
  BRONZE2(600, 799),
  BRONZE1(800, 999),
  
  SILVER5(1000, 1199),
  SILVER4(1200, 1399),
  SILVER3(1400, 1599),
  SILVER2(1600, 1799),
  SILVER1(1800, 1999),
  
  GOLD5(2000, 2199),
  GOLD4(2200, 2399),
  GOLD3(2400, 2599),
  GOLD2(2600, 2799),
  GOLD1(2800, 2999),
  
  PLATINUM5(3000, 3199),
  PLATINUM4(3200, 3399),
  PLATINUM3(3400, 3599),
  PLATINUM2(3600, 3799),
  PLATINUM1(3800, 3999),
  
  DIAMOND5(4000, 4199),
  DIAMOND4(4200, 4399),
  DIAMOND3(4400, 4599),
  DIAMOND2(4600, 4799),
  DIAMOND1(4800, 4999),
	MASTER(5000, Integer.MAX_VALUE);

	private final int minRating;
	private final int maxRating;

	UserTier(int minRating, int maxRating) {
		this.minRating = minRating;
		this.maxRating = maxRating;
	}

	public static UserTier getTierByRating(int rating) {
		for (UserTier tier : UserTier.values()) {
			if (rating >= tier.minRating && rating <= tier.maxRating) {
				return tier;
			}
		}
		return BRONZE5;
	}

	public int getMinRating() {
		return minRating;
	}
}
