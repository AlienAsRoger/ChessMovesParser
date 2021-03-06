package com.developer4droid.myapplication2.app;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

	private HashMap<String, String> annotationsMapping;
	private static final String COMMENT_START = "{ ";
	private static final String COMMENT_CLOSE = " }";
	private static final String ALTERNATE_START = "( ";
	private static final String ALTERNATE_CLOSE = " )";
	public static final String NUMBER_SAN_PATTERN = "[0-9]{1,4}[.]{1,3}[\\s][KQNBR]?([a-h]{0,2}([1-8]|([18]=[KQNBR])|([18][KQNBR]))|(x[a-h]|([a-h])x(?!\\5)[a-h]|([1-8])x[a-h](?!\\6))([1-8]|[18]=[KQNBR])(\\(ep\\))?)\\+{0,2}!{0,2}$|^[0-9]{1,4}[.]{1,3}[\\s]O-O(-O)?";
	public static final String SAN_PATTERN = "^[KQNBR]?([a-h]{0,2}([1-8]|([18]=[KQNBR])|([18][KQNBR]))|(x[a-h]|([a-h])x(?!\\5)[a-h]|([1-8])x[a-h](?!\\6))([1-8]|[18]=[KQNBR])(\\(ep\\))?)\\+{0,2}!{0,2}$|^O-O(-O)?$";


	private TextView textView;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.button).setOnClickListener(this);
		textView = (TextView) findViewById(R.id.textView);

		annotationsMapping = new HashMap<String, String>();
		fillMapping();

		test();
	}

	private void test() {
		HashMap<String, String> map = new HashMap<String, String>();

		String finalMoves = removeCommentsAndAlternatesFromMovesList(movesList, map);
//		String finalMoves = mapAlternateMoves(movesList, "", map);
		Log.d("TEST", finalMoves);
		textView.setText(finalMoves);
	}

	public String mapAlternateMoves(String movesList, String moveBeforeAlternative, HashMap<String, String> map) {

		// check if parenthesis inside of { }, if it's then skip as it is a comment and not alternative moves
		int commentCloseIndex = calculateCorrectBlockEndIndex(movesList, COMMENT_START, COMMENT_CLOSE);

		int start = movesList.indexOf(ALTERNATE_START);
		// check if there are more alternate moves inside of this moves sequence
		int end = calculateCorrectBlockEndIndex(movesList, ALTERNATE_START, ALTERNATE_CLOSE);

		// no need to parse anything
		if (end == 0) {
			return movesList;
		}

		if (isUnderComment(movesList, start)) { // if parenthesis inside of comments -> { () }
			// then search next parenthesis after comment -> { ( ( ) ( ) ) } ( )
			String commentString = movesList.substring(0, commentCloseIndex + 1); // we skip comment block and move caret to next opening par-s
			String tempMovesList = movesList.substring(commentCloseIndex + 1); // move next part of string for analysis further

			int alternativeBlockStartIndex = tempMovesList.indexOf(ALTERNATE_START);// in temp movesList
			// now we check if next "(" is inside of comment and if it is, when skip till the next alternative moves block
			while(isUnderComment(tempMovesList, alternativeBlockStartIndex)) { // start recursion
				// find next closing comment symbol
				commentCloseIndex = calculateCorrectBlockEndIndex(tempMovesList, COMMENT_START, COMMENT_CLOSE);
				if (tempMovesList.length() <= commentCloseIndex) { // if we reached the end of
					return movesList;
				}
				tempMovesList = tempMovesList.substring(commentCloseIndex + 1);
				commentString += tempMovesList.substring(0, commentCloseIndex + 1);
				alternativeBlockStartIndex = tempMovesList.indexOf(ALTERNATE_START); //
			}
			int commentLength = commentString.length();
			start = tempMovesList.indexOf(ALTERNATE_START) + commentLength; // re-calculate start of new opening parenthesis
			// check if there are more alternate moves inside of this moves sequence
			end = calculateCorrectBlockEndIndex(tempMovesList, ALTERNATE_START, ALTERNATE_CLOSE) + commentLength; // recalculate of new closing parenthesis
			// no need to parse anything
			if (end == 0) {
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
			substring = movesList.substring(end + 1);  // next part of MovesList that will be parsed further
		}

		if (map != null) { // only if we need those moves
			if (start - 2 < 0) { // if there is no move before "("
				String previousAlternativeMoves = map.get(moveBeforeAlternative);
				alternateMoves = previousAlternativeMoves + " " + alternateMoves;
				map.put(moveBeforeAlternative, alternateMoves);
			} else { // save alternative moves in map with move before it
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

						break;
					} else {
						tempMove += charAt;
					}
				}
				moveBeforeAlternative = new StringBuilder(tempMove).reverse().toString(); // reverse saved temp move

				map.put(moveBeforeAlternative, alternateMoves);
			}
		}

		String moves = mapAlternateMoves(substring, moveBeforeAlternative, map);
		return clearMoves + moves;
	}

	/**
	 * Search for single opening comment symbol
	 * @param movesList
	 * @param startIndex
	 * @return {@code true} if amount of open"{" symbol doesn't match closed "}"
	 */
	private boolean isUnderComment(String movesList, int startIndex) {
		int openCnt = 0;
		int closeCnt = 0;
		for(int i = startIndex; i > 0; i--) {
			if (movesList.charAt(i) == '{') { // if in this string we found an opening comment, then we shouldn't mark this block as alternative moves
				openCnt++;
			}
			if (movesList.charAt(i) == '}') {
				closeCnt++;
			}
		}
		return openCnt > closeCnt;
	}

	/**
	 *
	 * @param movesList
	 * @param startSymbol must be 2 chars length
	 * @param endSymbol must be 2 chars length
	 * @return
	 */
	private int calculateCorrectBlockEndIndex(String movesList, String startSymbol, String endSymbol) {
		int start = movesList.indexOf(startSymbol);
		int end = movesList.indexOf(endSymbol) + 1;
		if (start == -1) { // if no parenthesis, quit
			return end;
		}

		if (end == 0) { // if there is no closing par-s
			end = movesList.length() - 1;
		}
		int openingsCnt = 0;
		// count opening par-s till the first closing par-s
		for (int z = 0; z < end; z++) {
			if (movesList.charAt(z) == startSymbol.charAt(0) && movesList.charAt(z + 1) == startSymbol.charAt(1)) {
				openingsCnt++;
			}
		}

		if (openingsCnt > 1) { // if there are ( (() ) )
			openingsCnt = 0; // drop to calculate correctly
			int closeCnt = 0;
			int z = 0;
			while (z < movesList.length()) {
				if (z+1 >= movesList.length()) { // because we are comparing 2 chars
					break;
				}
				// count opening par-s till we get same amount of blocks
				if (movesList.charAt(z) == startSymbol.charAt(0) && movesList.charAt(z + 1) == startSymbol.charAt(1)) {
					openingsCnt++;
					z++;
				}
				if (movesList.charAt(z) == endSymbol.charAt(0) && movesList.charAt(z + 1) == endSymbol.charAt(1)) {
					closeCnt++;
					z++;
				}
				if (closeCnt == openingsCnt && closeCnt != 0) { // we reached same amount of opened par-s, break
					break;
				}
				z++;
			}
			if (closeCnt < openingsCnt) { // if author forgot to put correct closing parenthesis
				z = movesList.length() - 1;
			}
			end = z;
		}

		return end;
	}

	public String replaceSpecialSymbols(String movesList) {
		for (String key : annotationsMapping.keySet()) {
			movesList = movesList.replaceAll(key, annotationsMapping.get(key));
		}
		return movesList;
	}

	public String removeCommentsAndAlternatesFromMovesList(String movesList, HashMap<String, String> alterMovesMap) {
		// replace all special symbols
		movesList = replaceSpecialSymbols(movesList);

		if (movesList.contains(ALTERNATE_START)) {
			movesList = mapAlternateMoves(movesList, Symbol.EMPTY, alterMovesMap);
		}

		while (movesList.contains(COMMENT_START)) {
			int firstIndex = movesList.indexOf(COMMENT_START);
			int lastIndex = movesList.indexOf(COMMENT_CLOSE) + 1; // because there are space in front of } " }"

			if (lastIndex == 0) {
				lastIndex = movesList.length();
			}
			String result = movesList.substring(firstIndex, lastIndex + 1);
			movesList = movesList.replace(result, Symbol.EMPTY);
		}

		return movesList.trim();
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

//		String movesList = "16...Ng4 $1 { Threats like 17...Qh4 and 17...Qf6 are very annoying. } 17.Rg1 ( 17.h3 Ne5 18.Qb5 Qh4 { leaves White in serious trouble since } 19.c5 $2 Bf5 { is completely winning. } ) ( 17.Bb2 Qh4 18.Rf1 { was White's best defense, though Black's initiative still gives him all the chances. } ) 17...Qf6 18.Rg2 Qa1+ 19.Ke2 Ne5 20.f4 Nxc4 { Of course,  } ( 20...Nxc6 { also won. } ) 21.Qh5 ( 21.Qxc7 Nxa3 22.Qxd6 Bh3 23.Rf2 Nc2 { is crushing. } )  21...Nxa3 22.Be4 g6 23.Qa5 Bh3 { , 0-1. }";
//		String movesList = "46...Bb6 $1 47.Bxb6 ( 47.c7 Bxe3 48.d7 ( 48.Rxf2 Bxf2 49.d7 Rg8 $1 50.h4 Rg1+ 51.Kh2 Bg3+ $1 52.Kxg1 Bxc7 ) 48...Rg8 49.h4 ( 49.d8=Q Rg1+ 50.Rxg1 fxg1=Q# ) 49...Rg1+ 50.Kh2 Rxf1 51.d8=Q Rh1+ 52.Kg3 ( 52.Kxh1 f1=Q+ 53.Kh2 Bf4# ) 52...Rg1+ 53.Kh2 Bf4+ ( 53...f1=N+ 54.Kh3 Rg3# ) 54.Kh3 f1=Q# ) 47...axb6 48.Rxf2 e3 $3 49.Rxf8 d2 50.c7 d1=Q+ 51.Kg2 Qg4+ 52.Kf1 Qc4+ 53.Kg2 e2 54.Kf2 ( 54.c8=Q Qxc8 55.Rxc8 e1=Q 56.Rf8 Qd2+ 57.Rf2 Qxd6 ) 54...Qe6 55.Ke1 Qxd6 56.Rh8+ Kg6 $1 ( 56...Kxh8 $4 57.c8=Q+ Kh7 58.Qc2+ ) 57.Rg8+ Kh5 { , 0-1. } ";
//		String movesList = "12.Bd3 Qb6 { Black has a wealth of good choices: } ( 12...Qh4+ 13.g3 Qe7 ) ( 12...Rb8 { and even } ) ( 12...O-O { since } 13.O-O { allows } 13...Qb6+ { picking up the b4-pawn. } ) 13.Rb1 O-O ( 13...Qe3+ 14.Ne2 g5 { undermining the e5-pawn is also good. } ) 14.Qe2 f6 ( 14...a5 $1 { is probably even stronger. I'm just trying to show that Black has far more dynamic options than White has. } ) 15.Qh5 f5 16.Qe2 Qd4 17.Qd2 a5 18.a3 axb4 19.axb4 Ra3 20.Ne2 Qa7 { and Black's in command (White's king is in the middle and Black's pawn breaks (...c6-c5 and ...g7-g5) are itching to be played. } ";
//	String movesList = "24.Kg1⁈ { Karpov does little irreversible damage with this move, but it is\n" +
//		"certainly a step in the wrong direction. In order to neutralize Black's\n" +
//		"dangerous central operation, energetic and accurate play was required: } ( 24.Bd4! Bxd4 25.Nxd4 e5! 26.fxe5 ( 26.Qh4⁉ Nf8 27.Nde2 exf4 28.Nxf4 Rce8 { Once again, White is not objectively worse, but Black's central pressure\n" +
//		"guarantees him very comfortable play for the foreseeable future. } ) 26...dxe5 27.Nf3 ( 27.Nb3 Rf8! 28.Qd2 f6 ) 27...Rf8 28.Qh4 f6 29.gxf6 Nxf6 30.Ng5 Nh5 31.Rxf8+ Kxf8 32.Bf3 Nf6 { White maintains certain attacking\n" +
//		"chances, but with excellent piece coordination and steady central pressure,\n" +
//		"Black is certainly not worse. Of course, such a position did not suit Karpov\n" +
//		"-  if he is circumspect, Black is hardly in danger of losing. } ) ( 24.Rd1 f5 25.gxf6 Nxf6⁉ 26.Bxb6 Ng4 27.Qg1 Qb8 ) 24...Rce8 { Black follows through\n" +
//		"on his plan (and, as a bonus, stops f4-f5 once and for all), but as Kasparov\n" +
//		"notes, there was no need for such thorough preparation. The immediate } ( 24...f5! { would have confronted White with serious problems. For instance, } 25.gxf6 Bxf6 26.Qd2 ( 26.f5 exf5 27.exf5 Bxg2 28.Qxg2 Qc6 { (Kasparov) } ) 26...Bg7 27.f5 exf5 28.exf5 Bxg2 29.Qxg2 Qc6 30.Qg5 Rce8 { and despite the\n" +
//		"extreme complexity of the position, it appears that White can only level the\n" +
//		"boat through a series of only computer moves. I have no interest in\n" +
//		"over-analyzing this position: it is clear that ...f5 would have posed serious\n" +
//		"objective problems. } ) 25.Rd1 f5! { Black's plan has worked perfectly, but\n" +
//		"White has improved his position just enough to neutralize the pressure. } 26.gxf6 Nxf6 { Bravely played! Black sacrifices a pawn, relying on the activity of\n" +
//		"his pieces to serve as adequate compensation. Unwilling to calculate the\n" +
//		"ramifications of 27.Bxb6, Karpov makes a neutral move: } 27.Rg3 { An indolent\n" +
//		"move after which Kasparov's pieces come into play with savage effect. To be\n" +
//		"sure, White's position is still very much playable, but the real test of\n" +
//		"Black's strategy was the \"greedy\" } ";

//		String movesList = "23...Re7 $3 { White's troubles are exacerbated by the fact that he really has\n" +
//			"no effective way of stopping Black's plan (...Rce8 followed by ...f5). Not\n" +
//			"sensing the danger, Karpov continues playing in his characteristically\n" +
//			"unhurried style. } ( 23...Nc5 $2 24.Bd4 $1 { and Black is forced to acquiesce\n" +
//			"to the higly undesirable trade of dark-squared bishops: } 24...Bxd4 ( 24...e5 25.fxe5 Bxe5 ( 25...dxe5 26.Bxc5 bxc5 27.Qh4 h5 28.gxh6 Bh8 29.h7+ Kf8 30.Rhf3 ) 26.Qh4 h5 27.gxh6 Bxd4 28.Nxd4 Qe7 29.Qe1 { Black's king might\n" +
//			"not be in immediate danger, but it is only a matter of time before White\n" +
//			"regroups his forces and crashes through along the f-file. } ) 25.Nxd4 Qe7 26.f5 $1 e5 27.f6 Qf8 28.Nf5 $1 { with a fearsome attack. In fact, I cannot see how\n" +
//			"Black actually survives for more than a few moves! } ) ( 23...f5 24.gxf6 Bxf6 25.Bd4 Rf8 { The computer is quite optimistic about Black's chances (he\n" +
//			"evaluates the position as approximately equal), but I believe that it is\n" +
//			"foolish to idolize Houdini in such a position. From a practical standpoint,\n" +
//			"after } 26.Bxf6 Nxf6 27.Nd4 Rce8 28.Qh4 { I would certainly prefer White. } ) 24.Kg1 $6 { Karpov does little irreversible damage with this move, but it is\n" +
//			"certainly a step in the wrong direction. In order to neutralize Black's\n" +
//			"dangerous central operation, energetic and accurate play was required: } ( 24.Bd4 $1 Bxd4 25.Nxd4 e5 $1 26.fxe5 ( 26.Qh4 $5 Nf8 27.Nde2 exf4 28.Nxf4 Rce8 { Once again, White is not objectively worse, but Black's central pressure\n" +
//			"guarantees him very comfortable play for the foreseeable future. } ) 26...dxe5 27.Nf3 ( 27.Nb3 Rf8 $1 28.Qd2 f6 ) 27...Rf8 28.Qh4 f6 29.gxf6 Nxf6 30.Ng5 Nh5 31.Rxf8+ Kxf8 32.Bf3 Nf6 { White maintains certain attacking\n" +
//			"chances, but with excellent piece coordination and steady central pressure,\n" +
//			"Black is certainly not worse. Of course, such a position did not suit Karpov\n" +
//			"-  if he is circumspect, Black is hardly in danger of losing. } ) ( 24.Rd1 f5 25.gxf6 Nxf6 $5 26.Bxb6 Ng4 27.Qg1 Qb8 ) 24...Rce8 { Black follows through\n" +
//			"on his plan (and, as a bonus, stops f4-f5 once and for all), but as Kasparov\n" +
//			"notes, there was no need for such thorough preparation. The immediate } ( 24...f5 $1 { would have confronted White with serious problems. For instance, } 25.gxf6 Bxf6 26.Qd2 ( 26.f5 exf5 27.exf5 Bxg2 28.Qxg2 Qc6 { (Kasparov) } ) 26...Bg7 27.f5 exf5 28.exf5 Bxg2 29.Qxg2 Qc6 30.Qg5 Rce8 { and despite the\n" +
//			"extreme complexity of the position, it appears that White can only level the\n" +
//			"boat through a series of only computer moves. I have no interest in\n" +
//			"over-analyzing this position: it is clear that ...f5 would have posed serious\n" +
//			"objective problems. } ) 25.Rd1 f5 $1 { Black's plan has worked perfectly, but\n" +
//			"White has improved his position just enough to neutralize the pressure. } 26.gxf6 Nxf6 { Bravely played! Black sacrifices a pawn, relying on the activity of\n" +
//			"his pieces to serve as adequate compensation. Unwilling to calculate the\n" +
//			"ramifications of 27.Bxb6, Karpov makes a neutral move: } 27.Rg3 { An indolent\n" +
//			"move after which Kasparov's pieces come into play with savage effect. To be\n" +
//			"sure, White's position is still very much playable, but the real test of\n" +
//			"Black's strategy was the \"greedy\" } ( 27.Bxb6 $1 { After } 27...Ng4 28.Bxc7 Nxf2 29.Bxd6 Nxd1 30.Bxe7 Nxc3 $1 { During the game, Karpov feared } ( 30...Rxe7 31.Nxd1 Nxc2 { with seemingly powerful compensation for the pawn, but the\n" +
//			"level-headed } 32.e5 $1 { , given by Kasparov, puts an end to Black's dreams.\n" +
//			"Once again, I have no intention of boring you with a long investigation of the\n" +
//			"position; suffice it to say that after } 32...Nb4 33.Nc3 Bxg2 34.Kxg2 Rf7 35.Rf3 g5 { Black has decent drawing chances, but this is certainly not the kind of\n" +
//			"position Kasparov had in mind when sacrificing a pawn! } ) 31.bxc3 Nxc2 32.Bd6 { and now, according to Kasparov, the powerful } 32...e5 $1 { highlights the weakness\n" +
//			"of White's central pawns and gives Black excellent drawing chances. Still, as\n" +
//			"we will see, Karpov should have chosen this position over the text! } ) 27...Rf7 { Simple and strong. Kasparov leaves the b6 pawn under fire, but x-rays the\n" +
//			"queen and prepares ...Nh5. Slowly but steadily, Karpov begins to lose control\n" +
//			"of the game. } 28.Bxb6 Qb8 29.Be3 Nh5 $1 { A terrific resource. Kasparov\n" +
//			"targets the weakest point in White's camp (the f4) pawn, and nearly forces a\n" +
//			"repetition of moves. Needing a win, Karpov burns all the bridges and allows a\n" +
//			"dazzling combinative sequence. } 30.Rg4 ( 30.Rf3 { For the longest time, I\n" +
//			"could not, for the life of me, understand why Karpov did not make this\n" +
//			"entirely natural move. It leaves the e4 pawn suspiciously weak, but Black\n" +
//			"cannot exploit that... } 30...Bxc3 $1 31.bxc3 Na2 { Or can he? This sequence sure\n" +
//			"looks computeresque, but of course Kasparov had to foresee it during the game.\n" +
//			"Black has given up the pride of his bishop, but with the c3 and e4 pawns\n" +
//			"hanging, White's position simply falls apart. } 32.Qd2 Qc7 $1 { Much stronger\n" +
//			"than the rash } ( 32...Bxe4 $6 33.Ra1 $1 Nxc3 34.Qxc3 Bxf3 35.Bxf3 Nxf4 36.Nd4 { with good counterchances. } ) 33.Qxd6 Bxe4 34.Qxc7 Rxc7 35.Rf2 Nxc3 { White's pawn structure is dreadfully weak, his pieces are permanently\n" +
//			"uncoordinated, and he can barely even move. Of course, going for such a\n" +
//			"miserable position was out of the question for Karpov. } ) 30...Nf6 { Now, the\n" +
//			"most prudent continuation would have been to repeat moves with 31.Rg3, but\n" +
//			"Karpov decides to risk it all in a desperate attempt to muddy the waters. } 31.Rh4 $6 { Of course, given the circumstances, Karpov did not really have a\n" +
//			"choice, but now Black tears open the f-file with great effect: } 31...g5 $1 32.fxg5 Ng4 ( 32...Nxe4 $2 { This looks tempting at first, but in fact White weathers\n" +
//			"the storm with } 33.Qe2 Nxc3 34.bxc3 Nd5 35.Rf1 $1 Rxf1+ 36.Bxf1 Nxe3 37.Qxe3 { and it turns out that White has developed some formidable attacking\n" +
//			"chances connected with the threats of Bd3, Bc4, and Qh3. } ) 33.Qd2 Nxe3 34.Qxe3 { With the e3 bishop gone, White's position hangs on a thread. } 34...Nxc2 35.Qb6 Ba8 $1 { Admirable precision! The queen trade is clearly the objectively\n" +
//			"best move for White, but, once again, you must remember the circumstances  -  a\n" +
//			"draw for Karpov was tantamount to a loss. } 36.Rxd6 Rb7 37.Qxa6 Rxb3 $6 { Finally, the pressure begins to take its toll. This move is viable, but the\n" +
//			"prosaic } ( 37...Nb4 $1 { won a rook on the spot. } ) 38.Rxe6 Rxb2 { Oddly enough,\n" +
//			"this natural move actually allows a draw. After the more precise } ( 38...Ne3 $1 39.Rxe8+ Qxe8 40.Qe2 Rb4 $5 { , White would have remained a piece down for\n" +
//			"little compensation. } ) 39.Qc4 ( 39.Nd1 ) 39...Kh8 40.e5 $2 { Karpov, in\n" +
//			"despair, loses all hope and commits hara-kiri. In fact, the incredibly\n" +
//			"cold-blooded } ( 40.Rxe8+ Qxe8 41.Nd1 $1 { would have saved the game (but not\n" +
//			"the match, of course, so this is technically a moot point): } 41...Na3 42.Qd3 Ra2 43.g6 $1 h6 { and now the improbable } 44.Rxh6+ $1 { achieves perpetual: } 44...Bxh6 45.Qc3+ Bg7 ( 45...Kg8 46.Qb3+ Kg7 47.Qxa2 Qd7 48.Nf2 { and White can\n" +
//			"suddenly play for a win! } ) 46.Qh3+ Kg8 47.Qb3+ Kf8 48.Qb4+ Qe7 49.Qb8+ Qe8 50.Qb4+ ) 40...Qa7+ $1 { An elegant finishing touch. White's position\n" +
//			"comes apart at the seams. } 41.Kh1 Bxg2+ 42.Kxg2 Nd4+ ";

//	String movesList = "30.bxa3 Rd8 31.Rb2 Rc7 32.Rb5 Rdd7 33.Kf2 { Another sign of finesse: Rozentalis does\n" +
//			"not hurry to double on the b-file. Who knows - maybe the rook will be needed\n" +
//			"on f1 to prevent ...Kf7 or attack the f5 pawn. } 33...g6 34.Ke3 Kg7 35.Rfb1 Kf7 36.Rc5! { Another beautiful concept, although straightforward play seems to do\n" +
//			"the trick as well. Taking the rook is tantamount to suicide (the White king is\n" +
//			"granted a monstrous outpost on d4), so Black must allow a further penetration. } ( 36.Bf3⁉ Ke7 37.Kf4 h6 38.a6! bxa6 ( 38...b6 39.a4 { with the crushing\n" +
//			"a5 to follow. } ) 39.Ra5 Rc6 40.Bd1 Rb6 41.Rxb6 axb6 42.Rxa6 b5 43.g4 { and\n" +
//			"Black should not be able to hold his position for much longer. } 43...Rd8 ) 36...Ke7 37.Rbb5 Rxc5 38.dxc5! Kd8 { This allows a pretty finale, but Black can\n" +
//			"hardly be critcized for not putting up a tenacious resistance in such a\n" +
//			"miserable position. } ( 38...Rc7! { was the only way to prolong the fight,\n" +
//			"although White still wins with } 39.Kd4 Bd7 40.Rb4 Bc6 41.a6‼ bxa6 42.Rb8 { and the dam bursts. } ) 39.a6! Kc8 ( 39...bxa6 40.Ra5 ) 40.Rb6‼ { Rozentalis outdoes himself yet again! The rook is untouchable, and Black is\n" +
//			"forced to make yet another  -  fatal  -  concession. } 40...Bg8 ( 40...axb6 41.a7 { and the pawn is unstoppable. A position that truly epitomizes the beauty of\n" +
//			"chess, wouldn't you say? } ) 41.Rf6 ( 41.e6! Re7 42.axb7+ Kb8 43.Rd6 Bxe6 44.c6 { was even more accurate. } ) 41...Rd8 42.Kd4 bxa6 43.Rd6";
//	String movesList = "26.Qd1 $1 Rc8 27.a5 { For the moment, White grabs much-needed space and bides\n" +
//			"his time (the a5 pawn will always be safely defended by the queen from a1).\n" +
//			"The immediate } ( 27.b3 $2 { fails to impress after } 27...cxb3 28.Qxb3 Rc4 29.a5 Qd8 { when it is clear that White has acted prematurely. } ) 27...Rcf8 $2 { In fact,\n" +
//			"this is a serious error which allows White to execute his stupendous plan\n" +
//			"undeterred. At first sight, it appears that Black can afford to move his\n" +
//			"pieces back  -   his position is extremely solid. 28.b3 is a shot into thin air\n" +
//			"(see note to White's 28th move). Once again, Rozentalis determines that to\n" +
//			"make progress, he must improve all of his pieces to the maximum. The rooks and\n" +
//			"bishop are doing all they can, but the queen is still quite passive. Can it be\n" +
//			"rerouted to a better square? You bet! } ( 27...g6 { In perfect hindsight, it is\n" +
//			"clear that Black should have paid more heed to White's queenside expansion. If\n" +
//			"he keeps his queen on d8 and rook on c6, it turns out that White has a very\n" +
//			"difficult time making any dents on the queenside (...g6 is played to free the\n" +
//			"queen from the onerous task of defending f5). } 28.Qa1 Rc6 $1 29.Qa3 Qd8 { and\n" +
//			"while White can certainly try to play b2-b3 at a timely moment, Black's\n" +
//			"chances to hold are very high. } ) 28.Qa1 $1 Qe7 { As the Russians would say,\n" +
//			"this is the \"salt\" (i.e. the fine point, the justification) of Black's idea.\n" +
//			"Both the a3 and a4 squares (due to ...Bd7) are taken away from White's queen,\n" +
//			"and the threat of ...f4 is a force to be reckoned with. Recall, however, that\n" +
//			"the original idea of White's whole queenside operation was to open the b-file.\n" +
//			"And is b2-b3 the only way to do so? } 29.Qa3 $3 { Absolutely not!! A move of\n" +
//			"astounding depth. Considering that White's pawn structure is irreparably\n" +
//			"ruined after the queen trade, this is an incredibly difficult move to make,\n" +
//			"but a GM ain't a GM for nothing! The opening of the b-file  -  ruined pawn\n" +
//			"structure aside  -  will allow the White rooks to pressure b7 and d5 (from b5)\n" +
//			"simultaneously. Meanwhile, the passivity of Black's pieces will doom any\n" +
//			"long-term defense from the start. Appel fails to acknowledge the correctness\n" +
//			"of White's idea, and allows Rozentalis to bring the game to an astounding\n" +
//			"conclusion. } 29...Qxa3 $2 { A myopic decision. The endgame will be objectively\n" +
//			"defensible (i.e. Black does not lose by force), but from a practical\n" +
//			"standpoint, flawless defense will be nearly impossible. As GM Rowson notes, } ( 29...Rd8 $1 { was far more resilient. Now, White can choose between a few ways\n" +
//			"to keep the pressure going: } 30.Qxe7 { This is the computer's recommendation,\n" +
//			"and I think the queen certainly does not favor Black. After } ( 30.Ra1 $5 { is\n" +
//			"Rowson's suggestion, but I am not sure it is a good idea to give Black the\n" +
//			"opportunity to refuse the queen trade. Even so, after } 30...Qe8 31.Qb4 g6 32.Bf3 Qd7 { White is certainly better, but at least Black has succesfully regrouped. } ) 30...Rxe7 31.Bh3 g6 32.Kg2 Kg7 33.Kf3 Rc8 34.Kf4 Rc6 35.Ra1 { White will\n" +
//			"transfer his bishop to g2 and his rooks to a4 and a3 in preparation for b3. It\n" +
//			"is difficult to provide an accurate evaluation of the position, but I would\n" +
//			"put the winning chances/drawing chances relation at about 60%-40%. } ) 30.bxa3 Rd8 31.Rb2 Rc7 32.Rb5 Rdd7 33.Kf2 { Another sign of finesse: Rozentalis does\n" +
//			"not hurry to double on the b-file. Who knows - maybe the rook will be needed\n" +
//			"on f1 to prevent ...Kf7 or attack the f5 pawn. } 33...g6 34.Ke3 Kg7 35.Rfb1 Kf7 36.Rc5 $1 { Another beautiful concept, although straightforward play seems to do\n" +
//			"the trick as well. Taking the rook is tantamount to suicide (the White king is\n" +
//			"granted a monstrous outpost on d4), so Black must allow a further penetration. } ( 36.Bf3 $5 Ke7 37.Kf4 h6 38.a6 $1 bxa6 ( 38...b6 39.a4 { with the crushing\n" +
//			"a5 to follow. } ) 39.Ra5 Rc6 40.Bd1 Rb6 41.Rxb6 axb6 42.Rxa6 b5 43.g4 { and\n" +
//			"Black should not be able to hold his position for much longer. } 43...Rd8 ) 36...Ke7 37.Rbb5 Rxc5 38.dxc5 $1 Kd8 { This allows a pretty finale, but Black can\n" +
//			"hardly be critcized for not putting up a tenacious resistance in such a\n" +
//			"miserable position. } ( 38...Rc7 $1 { was the only way to prolong the fight,\n" +
//			"although White still wins with } 39.Kd4 Bd7 40.Rb4 Bc6 41.a6 $3 bxa6 42.Rb8 { and the dam bursts. } ) 39.a6 $1 Kc8 ( 39...bxa6 40.Ra5 ) 40.Rb6 $3 { Rozentalis outdoes himself yet again! The rook is untouchable, and Black is\n" +
//			"forced to make yet another  -  fatal  -  concession. } 40...Bg8 ( 40...axb6 41.a7 { and the pawn is unstoppable. A position that truly epitomizes the beauty of\n" +
//			"chess, wouldn't you say? } ) 41.Rf6 ( 41.e6 $1 Re7 42.axb7+ Kb8 43.Rd6 Bxe6 44.c6 { was even more accurate. } ) 41...Rd8 42.Kd4 bxa6 43.Rd6 ";

	String movesList = "{ The position is quite complex. Material is equal, but Black needs only two moves - ...Bb7 and ...Rfe8 - to set his formidable center in motion (...e5-e4 will be rather unpleasant). It is clear that White must act before Black has finished his development, and it is also clear that all he has all necessary prerequisites - bishop on d3, knight on f3, queen on e2 - to sacrifice on h7. However, two factors seem to make this sacrifice impracticable. First, Black has a bishop, rather than a pawn, on g7. The bishop is obviously a stronger defensive piece, and could disrupt the coordination between White's queen and knight. Second, Black has plenty of pieces ready to defend the king at a moment's notice. The queen, for example, needs only two moves to reach the kingside (...e5 and ...Qg(f,h)6. However, as I've mentioned, the queen and knight are such a potent force that they can frequently override even the most stalwart of defenses. The Czech master does not hesitate, although the sacrifice engenders nearly inscrutable tactical complications: } 16.Bxh7+ $1 Kxh7 17.Ng5+ Kg6 $1 { This move is never pleasant to make, but the \"natural\" 17...Kg8 actually plays into White's hands. Now, Black's king has a prime escape route through f6. } ( 17...Kg8 $2 18.Qh5 Rd8 19.Qxf7+ Kh8 20.Bf4 $5 { With Black's king so weakened, White can afford the luxury of developing another piece. } ( 20.f4 $6 { (intending Rf3) is a worthy alternative, but Vuckovic correctly notes that } 20...Qb7 21.Qg6 ( 21.Bd2 $5 d3 22.Rf3 Qxf7 23.Nxf7+ Kh7 24.Nxd8 Bxb2 25.Rd1 e5 26.f5 $1 Bxf5 ( 26...e4 27.Rh3+ Kg7 28.Rg3+ Kf8 29.Be3 { and White should wind up victorious despite Black's dangerous pair of connected passers. } ) 27.Rxf5 Rxd8 28.Kf1 Na4 $1 { and although White is undoubtedly objectively better, Black's practical counterchances are huge. } ) 21...Kg8 { and now instead of Vuckovic's suggestion of 22.Rf3, I think that the simple developing move } 22.Bd2 $1 { is impossible to deal with. For instance, } 22...Ra6 23.Rac1 $1 { (Notice how White conducts the attack with extraordinary energy; every move comes with gain of tempo, every move forces Black to overextend a little bit more) } 23...Qe7 ( 23...Rc6 24.Qh7+ Kf8 25.f5 $1 exf5 26.Rxf5+ Bxf5 27.Qxf5+ Kg8 28.Qh7+ Kf8 { and now White has a beautiful way to win the game, although the prosaic 29.Rf1+ does the trick as well: } 29.Rxc5 $3 Rxc5 30.Ne6+ Ke8 31.Qg8+ Ke7 32.Qxg7+ { and Black loses his queen. } ) 24.Bb4 Rc6 25.f5 $1 exf5 26.Qxc6 Qxg5 27.Bxc5 { and in addition to White's overwhelming material advantage, the attack rages on with full force. } ) 20...e5 { Black desperately tries to bring his queen into the defense, but to no avail. } 21.Bxe5 Qh6 22.Qe7 $1 Be6 23.Bxg7+ Qxg7 24.Qxc5 Bd5 25.h4 { and White should easily convert his material advantage. } ) 18.Qg4 f5 19.Qg3 { Here Vuckovic claims that the most resilient defense was 19... Rf7!?, but I don't see anything wrong with the text move. It is important not to overestimate the potency of White's attack; with insanely accurate, computeresque defense, Black may well be able to save his king. In an actual game, with Black undoubtedly in time trouble, precise defense is nearly impossible. } 19...Kf6 ( 19...Rf7 20.b4 $1 { (Vuckovic) } 20...Nd7 { This is a lot more resilient than 20...Na4, but White's attack is still overwhelming after } ( 20...Na4 { and now I must quote Vuckovic's anlaysis, which is remarkably accurate for its time (he wrote in 1965!): } 21.h4 $1 e5 22.h5+ Kf6 23.Re1 Re7 ( 23...e4 24.Nxe4+ Ke7 25.Nf6+ $1 Kxf6 26.Qg6# ) 24.Bf4 { A beautiful way to continue the attack. Black cannot keep his center together. } 24...exf4 ( 24...Qd6 25.Ne4+ $1 fxe4 26.Qg6# ) 25.Nh7+ Kf7 26.Rxe7+ Kxe7 27.Qxg7+ { and White's attack, coupled with the far-advanced passer, should seal the deal. } ) 21.Nxe6+ Kh7 22.Ng5+ Kg8 23.Nxf7 Kxf7 24.Re1 { With several highly unpleasant ideas, one of them being Bf4 followed by Rac1-c7. } ) 20.Bf4 Ke7 { Through skillful defense, Black has managed to evacuate his king from the immediate danger zone, but now White switches his attention to the queenside and the center. } ( 20...Rg8 { This inhuman move is confidently given by Houdini with the evaluation \"0. 00\", but - and here I must show off a little bit - after a long analysis, I think that I managed to convince the stubborn Silicon fiend that all is not so simple! } 21.Rac1 $1 ( 21.Be5+ Ke7 22.Bxg7 Rxg7 23.Qh4 { This looks very strong, but now Black has the cold-blooded } 23...Nd7 $1 24.Qh6 Rxg5 25.Qxg5+ Kf7 { and I do not think White has much more than a draw by repetition (Qh5+-Qg5+). } ) 21...Ke7 ( 21...Bh6 22.Be5+ ( 22.h4 Ke7 23.Bc7 Ne4 24.Bxb6 Nxg3 25.fxg3 Rxg5 $5 26.hxg5 Bxg5 27.Bxd4 Bxc1 28.Rxc1 Ra4 { and Black should hold without much trouble. } ) 22...Ke7 23.Bxd4 Rxg5 24.Qh4 { and Black's position collapses! } ) ( 21...Bb7 22.Rxc5 $1 Qxc5 23.Rc1 Qxc1+ 24.Bxc1 Bd5 25.Nh7+ Kf7 26.Qc7+ Kg6 27.Ng5 { Once again, the attack should prove decisive. } ) 22.Bc7 Qc6 23.b4 Ne4 24.Nxe4 Qxe4 25.Bd6+ Kf6 26.Bf4 { Very subtle play: White draws the king to f6 before threatening Qg5+. } 26...Ra7 27.Rfe1 Qd5 28.Rxc8 $1 Rxc8 29.Be5+ { and White wins everything. } ) 21.Rac1 Ra7 $2 { Black tries to protect the seventh rank, but now White's pieces pour in from all sides. } ( 21...Bf6 { This was, I think, the most resilient defense. After } 22.Nh7 Ne4 23.Qg6 Qd8 { White must play very precisely in order to round off the attack: } 24.Rfe1 $3 { (the point of this mysterious move will be revealed soon) } 24...Bd7 25.Rxe4 $1 fxe4 { And now, ladies and gentleman, a move worth a million dollars: } 26.Bd2 $3 { So this was the point of Re1-xe4: with the d2 square undefended, White's bishop can transfer to the devastating square b4. } 26...Ra4 27.b3 e3 28.fxe3 dxe3 29.Bxe3 Bd4 { Black is able to trade the bishop, but at too great a cost: } 30.Bxd4 Rxd4 31.Qg5+ Ke8 32.Qxd8+ Kxd8 33.Nxf8 { and the endgame is completely winning. } ) 22.Rfe1 { Another cold-blooded and remarkably strong move. Black simply cannot keep his position together any longer. } 22...Bd7 ( 22...Bf6 { As Vuckovic notes, this no longer presents any trouble for White: } 23.Nh7 Rf7 24.Nxf6 Rxf6 25.Qg7+ Rf7 26.Qxd4 { crushing. } ) 23.b4 Na6 24.Nxe6 $1 { A final blow! Black's position comes apart in a picturesque manner: } 24...Bxe6 25.Qxg7+ Rf7 26.Bg5+ Kd7 27.Qh8 Qb8 28.Qxd4+ 1-0";
}
