package justfatlard.pillager_pirates.ship;

/**
 * The four hulls a pillager crew puts to sea in, and the dimensions each is drawn to.
 *
 * <p>Every ship is drawn bow toward +z with its keel on the centreline, and y measured from the
 * waterline: {@code y = 0} is the top layer of water, so the hull below it is what sits in the sea
 * and {@link #deck} is how high the main deck stands out of it.
 *
 * <p>{@link #tonnage} is the big-boats helm rating the ship is built to: a crew that takes one can sail
 * it away with a helm of that rating, and the one it carries is rated exactly that. {@code ShipTest}
 * holds every ship under the capacity big-boats gives that rating.
 *
 * <p>{@link #bilge} is the hull's cross-section from the keel up: how far each layer is pulled in from
 * the full beam. The first entry is always the keel itself, which is one block wide whatever it says.
 * Layers past the end of the array are full beam.
 */
public enum ShipType {
	SLOOP("sloop", 1, 14, 3, 2, 2, new int[] {3, 1}, 0.62),
	LONGSHIP("longship", 1, 19, 3, 1, 1, new int[] {3}, 0.9),
	BRIGANTINE("brigantine", 2, 24, 4, 3, 2, new int[] {4, 2, 1}, 0.62),
	GALLEON("galleon", 3, 32, 5, 4, 3, new int[] {5, 3, 2, 1}, 0.55);

	public final String id;
	public final int tonnage;
	public final int length;
	public final int halfBeam;
	public final int draft;
	public final int deck;
	private final int[] bilge;
	/**
	 * How sharply the bow closes in: 1 is a straight-sided wedge, lower is a fuller, rounder bow. A
	 * longship is nearly a wedge; a galleon is bluff.
	 */
	public final double bowFullness;

	ShipType(String id, int tonnage, int length, int halfBeam, int draft, int deck, int[] bilge, double bowFullness) {
		this.id = id;
		this.tonnage = tonnage;
		this.length = length;
		this.halfBeam = halfBeam;
		this.draft = draft;
		this.deck = deck;
		this.bilge = bilge;
		this.bowFullness = bowFullness;
	}

	public int keelY() {
		return -draft;
	}

	/** How far the layer at {@code y} is pulled in from the full beam. */
	public int inset(int y) {
		int layer = y - keelY();
		return layer < bilge.length ? bilge[layer] : 0;
	}

	public static ShipType byId(String id) {
		for (ShipType type : values()) {
			if (type.id.equals(id)) return type;
		}
		throw new IllegalArgumentException("Unknown ship type: " + id);
	}
}
