package com.developer4droid.myapplication2.app;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.HashMap;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

	private HashMap<String, String> annotationsMapping;
	private static final String COMMENT_START = "{";
	private static final String COMMENT_CLOSE = "}";
	private static final String ALTERNATE_MOVES_START = Symbol.LEFT_PAR;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.button).setOnClickListener(this);

		annotationsMapping = new HashMap<String, String>();
		fillMapping();

		test();

	}

	private void test() {
//		String movesList = "16...Ng4 $1 { Threats like 17...Qh4 and 17...Qf6 are very annoying. } 17.Rg1 ( 17.h3 Ne5 18.Qb5 Qh4 { leaves White in serious trouble since } 19.c5 $2 Bf5 { is completely winning. } ) ( 17.Bb2 Qh4 18.Rf1 { was White's best defense, though Black's initiative still gives him all the chances. } ) 17...Qf6 18.Rg2 Qa1+ 19.Ke2 Ne5 20.f4 Nxc4 { Of course,  } ( 20...Nxc6 { also won. } ) 21.Qh5 ( 21.Qxc7 Nxa3 22.Qxd6 Bh3 23.Rf2 Nc2 { is crushing. } )  21...Nxa3 22.Be4 g6 23.Qa5 Bh3 { , 0-1. }";
//		String movesList = "46...Bb6 $1 47.Bxb6 ( 47.c7 Bxe3 48.d7 ( 48.Rxf2 Bxf2 49.d7 Rg8 $1 50.h4 Rg1+ 51.Kh2 Bg3+ $1 52.Kxg1 Bxc7 ) 48...Rg8 49.h4 ( 49.d8=Q Rg1+ 50.Rxg1 fxg1=Q# ) 49...Rg1+ 50.Kh2 Rxf1 51.d8=Q Rh1+ 52.Kg3 ( 52.Kxh1 f1=Q+ 53.Kh2 Bf4# ) 52...Rg1+ 53.Kh2 Bf4+ ( 53...f1=N+ 54.Kh3 Rg3# ) 54.Kh3 f1=Q# ) 47...axb6 48.Rxf2 e3 $3 49.Rxf8 d2 50.c7 d1=Q+ 51.Kg2 Qg4+ 52.Kf1 Qc4+ 53.Kg2 e2 54.Kf2 ( 54.c8=Q Qxc8 55.Rxc8 e1=Q 56.Rf8 Qd2+ 57.Rf2 Qxd6 ) 54...Qe6 55.Ke1 Qxd6 56.Rh8+ Kg6 $1 ( 56...Kxh8 $4 57.c8=Q+ Kh7 58.Qc2+ ) 57.Rg8+ Kh5 { , 0-1. } ";
		String movesList = "12.Bd3 Qb6 { Black has a wealth of good choices: } ( 12...Qh4+ 13.g3 Qe7 ) ( 12...Rb8 { and even } ) ( 12...O-O { since } 13.O-O { allows } 13...Qb6+ { picking up the b4-pawn. } ) 13.Rb1 O-O ( 13...Qe3+ 14.Ne2 g5 { undermining the e5-pawn is also good. } ) 14.Qe2 f6 ( 14...a5 $1 { is probably even stronger. I'm just trying to show that Black has far more dynamic options than White has. } ) 15.Qh5 f5 16.Qe2 Qd4 17.Qd2 a5 18.a3 axb4 19.axb4 Ra3 20.Ne2 Qa7 { and Black's in command (White's king is in the middle and Black's pawn breaks (...c6-c5 and ...g7-g5) are itching to be played. } ";
		HashMap<String, String> map = new HashMap<String, String>();

		String finalMoves = removeCommentsAndAlternatesFromMovesList(movesList, map);
//		String finalMoves = removeAlternateMoves(movesList, "", map);
		Log.d("TEST", finalMoves);
	}


	private int calculateEndIndexOfAlternates(String movesList) {
		int start = movesList.indexOf(Symbol.LEFT_PAR);
		int end = movesList.indexOf(Symbol.RIGHT_PAR);
		if (start == -1) { // if no parenthesis, quit
			return end;
		}

		if (end == -1) { // if there is no closing par-s
			end = movesList.length() - 1;
		}
		int newEnd = end;
		int newStart = start;
		String tempMoveList = movesList;
		// if there are ( (1) (2) (3) ), then remove (1) (2) (3)
		int removedLength = 0;
		while (true) {
			String testMoveList = tempMoveList.substring(newStart + 1, newEnd);
			if (!testMoveList.contains(Symbol.LEFT_PAR)) { // if not contains start of new moves, then done
				break;
			}
			// remove first block
			int secondStart = testMoveList.indexOf(Symbol.LEFT_PAR);
			String blockToRemove = tempMoveList.substring(newStart + secondStart, newEnd + 1);
			removedLength += blockToRemove.length();
			tempMoveList = tempMoveList.replace(blockToRemove, Symbol.EMPTY);

			newStart = tempMoveList.indexOf(Symbol.LEFT_PAR);
			newEnd = tempMoveList.indexOf(Symbol.RIGHT_PAR);
			if (newEnd == -1) { // if there is no closing par-s
				newEnd = tempMoveList.length() - 1;
			}
		}

		end = newEnd + removedLength;
		return end;
	}

	public String removeCommentsAndAlternatesFromMovesList(String movesList, HashMap<String, String> alterMovesMap) {
		// replace all special symbols
		movesList = replaceSpecialSymbols(movesList);

		if (movesList.contains(ALTERNATE_MOVES_START)) {
			movesList = removeAlternateMoves(movesList, Symbol.EMPTY, alterMovesMap);
		}

		while (movesList.contains(COMMENT_START)) {
			int firstIndex = movesList.indexOf("{");
			int lastIndex = movesList.indexOf("}") + 1;

			String result = movesList.substring(firstIndex, lastIndex);
			movesList = movesList.replace(result, Symbol.EMPTY);
		}

		return movesList.trim();
	}

	public String replaceSpecialSymbols(String movesList) {
		for (String key : annotationsMapping.keySet()) {
			movesList = movesList.replaceAll(key, annotationsMapping.get(key));
		}
		return movesList;
	}

	public String removeAlternateMoves(String movesList, String moveBeforeAlternative, HashMap<String, String> map) {

		// check if parenthesis inside of { }, if it's then skip as it is a comment and not alternative moves
		int commentStartIndex = movesList.indexOf(COMMENT_START);
		int commentCloseIndex = movesList.indexOf(COMMENT_CLOSE);

		int start = movesList.indexOf(Symbol.LEFT_PAR);
		// check if there are more alternate moves inside of this moves sequence
		int end = calculateEndIndexOfAlternates(movesList);

		// no need to parse anything
		if (end == -1) {
			return movesList;
		}

		if (commentStartIndex < start && commentCloseIndex > start) { // if parenthesis inside of comments -> { () }
			// then search another parenthesis
			String tempString = movesList.substring(commentCloseIndex);
			start = tempString.indexOf(Symbol.LEFT_PAR);
			// check if there are more alternate moves inside of this moves sequence
			end = calculateEndIndexOfAlternates(tempString);
			// no need to parse anything
			if (end == -1) {
				return movesList;
			}
		}

		String substring;
		String clearMoves = Symbol.EMPTY;
		String alternateMoves = Symbol.EMPTY;
		// if closing parenthesis is closer then opening
		if (end < start || (end > 0 && start == -1)) {
			substring = movesList.substring(end + 1);
		} else {
			clearMoves = movesList.substring(0, start); // remove alternate moves and parse further
			alternateMoves = movesList.substring(start, end + 1);
			substring = movesList.substring(end + 1);
		}

		if (map != null) { // only if we need those moves
			if (start - 2 < 0) { // if there is no move before "("
				String previousAlternativeMoves = map.get(moveBeforeAlternative);
				alternateMoves = previousAlternativeMoves + " " + alternateMoves;
				map.put(moveBeforeAlternative, alternateMoves);
			} else { // save alternative moves in map with move before them
				String tempMove = Symbol.EMPTY;
				for (int i = start - 2; i >= 0; i--) { // iterate down chars, and look for space
					char charAt = movesList.charAt(i);
					if (charAt == '}') { // don't add comments
						do {
							i--;
						} while (i > 0 && movesList.charAt(i) != '{');
						i -= 2; // skip '{' & ' '
						i = i < 0 ? 0 : i;
						charAt = movesList.charAt(i);
					}

					if (charAt == ' ' && tempMove.length() != 0) { // skip chars till next space
						moveBeforeAlternative = new StringBuilder(tempMove).reverse().toString(); // reverse saved temp move
						map.put(moveBeforeAlternative, alternateMoves);
						break;
					} else {
						tempMove += charAt;
					}
				}
			}
		}

		String moves = removeAlternateMoves(substring, moveBeforeAlternative, map);
		return clearMoves + moves;
	}

/*
16...Ng4 $1 { Threats like 17...Qh4 and 17...Qf6 are very annoying. }

17.Rg1 ( 17.h3 Ne5 18.Qb5 Qh4 { leaves White in serious trouble since } 19.c5 $2 Bf5 { is completely winning. } ) ( 17.Bb2 Qh4 18.Rf1 { was White's best defense, though Black's initiative still gives him all the chances. } )
 17...Qf6 18.Rg2 Qa1+ 19.Ke2 Ne5 20.f4 Nxc4 { Of course,  } ( 20...Nxc6 { also won. } )
 21.Qh5 ( 21.Qxc7 Nxa3 22.Qxd6 Bh3 23.Rf2 Nc2 { is crushing. } )
  21...Nxa3 22.Be4 g6 23.Qa5 Bh3 { , 0-1. }
 */

/*
	46...Bb6 $1 47.Bxb6 ( 47.c7 Bxe3 48.d7 ( 48.Rxf2 Bxf2 49.d7 Rg8 $1 50.h4 Rg1+ 51.Kh2 Bg3+ $1 52.Kxg1 Bxc7 ) 48...Rg8 49.h4 ( 49.d8=Q Rg1+ 50.Rxg1 fxg1=Q# ) 49...Rg1+ 50.Kh2 Rxf1 51.d8=Q Rh1+ 52.Kg3 ( 52.Kxh1 f1=Q+ 53.Kh2 Bf4# ) 52...Rg1+ 53.Kh2 Bf4+ ( 53...f1=N+ 54.Kh3 Rg3# ) 54.Kh3 f1=Q# ) 47...axb6 48.Rxf2 e3 $3 49.Rxf8 d2 50.c7 d1=Q+ 51.Kg2 Qg4+ 52.Kf1 Qc4+ 53.Kg2 e2 54.Kf2 ( 54.c8=Q Qxc8 55.Rxc8 e1=Q 56.Rf8 Qd2+ 57.Rf2 Qxd6 ) 54...Qe6 55.Ke1 Qxd6 56.Rh8+ Kg6 $1 ( 56...Kxh8 $4 57.c8=Q+ Kh7 58.Qc2+ ) 57.Rg8+ Kh5 { , 0-1. }
	 */


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		test();

	}


	private void fillMapping() {
		annotationsMapping.put(" \\$1", "!");
		annotationsMapping.put(" \\$2", "?");
		annotationsMapping.put(" \\$3", "‼");
		annotationsMapping.put(" \\$4", "⁇");
		annotationsMapping.put(" \\$5", "⁉");
		annotationsMapping.put(" \\$6", "⁈");
		annotationsMapping.put(" \\$7", "□");
		annotationsMapping.put(" \\$10", "=");
		annotationsMapping.put(" \\$13", "∞");
		annotationsMapping.put(" \\$14", "⩲");
		annotationsMapping.put(" \\$15", "⩱");
		annotationsMapping.put(" \\$16", "±");
		annotationsMapping.put(" \\$17", "∓");
		annotationsMapping.put(" \\$18", "+−");
		annotationsMapping.put(" \\$19", "−+");
	}
}
