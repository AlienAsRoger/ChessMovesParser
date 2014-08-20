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
		HashMap<String, String> map = new HashMap<String, String>();

		String finalMoves = removeCommentsAndAlternatesFromMovesList(movesList, map);
//		String finalMoves = removeAlternateMoves(movesList, "", map);
		Log.d("TEST", finalMoves);
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

	private int calculateEndIndexOfAlternates(String movesList) {
		int start = movesList.indexOf(Symbol.LEFT_PAR);
		int end = movesList.indexOf(Symbol.RIGHT_PAR);
		if (start == -1) { // if no parenthesis, quit
			return end;
		}

		if (end == -1) { // if there is no closing par-s
			end = movesList.length() - 1;
		}
		int openingsCnt = 0;
		// count opening par-s till the first closing par-s
		for (int z = 0; z < end; z++) {
			if (movesList.charAt(z) == '(') {
				openingsCnt++;
			}
		}

		if (openingsCnt > 1) { // if there are ( (() ) )
			int closeCnt = 0;
			int z = 0;
			while (z < movesList.length()) {
				if (movesList.charAt(z) == ')') {
					closeCnt++;
				}
				if (closeCnt == openingsCnt) { // we reached same amount of opened par-s, break
					break;
				}
				z++;
			}
			end = z;
		}

		return end;
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
			// then search next parenthesis after comment -> { ( ( ) ( ) ) } ( )
			String tempString = movesList.substring(commentCloseIndex + 1);
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

//		String movesList = "16...Ng4 $1 { Threats like 17...Qh4 and 17...Qf6 are very annoying. } 17.Rg1 ( 17.h3 Ne5 18.Qb5 Qh4 { leaves White in serious trouble since } 19.c5 $2 Bf5 { is completely winning. } ) ( 17.Bb2 Qh4 18.Rf1 { was White's best defense, though Black's initiative still gives him all the chances. } ) 17...Qf6 18.Rg2 Qa1+ 19.Ke2 Ne5 20.f4 Nxc4 { Of course,  } ( 20...Nxc6 { also won. } ) 21.Qh5 ( 21.Qxc7 Nxa3 22.Qxd6 Bh3 23.Rf2 Nc2 { is crushing. } )  21...Nxa3 22.Be4 g6 23.Qa5 Bh3 { , 0-1. }";
//		String movesList = "46...Bb6 $1 47.Bxb6 ( 47.c7 Bxe3 48.d7 ( 48.Rxf2 Bxf2 49.d7 Rg8 $1 50.h4 Rg1+ 51.Kh2 Bg3+ $1 52.Kxg1 Bxc7 ) 48...Rg8 49.h4 ( 49.d8=Q Rg1+ 50.Rxg1 fxg1=Q# ) 49...Rg1+ 50.Kh2 Rxf1 51.d8=Q Rh1+ 52.Kg3 ( 52.Kxh1 f1=Q+ 53.Kh2 Bf4# ) 52...Rg1+ 53.Kh2 Bf4+ ( 53...f1=N+ 54.Kh3 Rg3# ) 54.Kh3 f1=Q# ) 47...axb6 48.Rxf2 e3 $3 49.Rxf8 d2 50.c7 d1=Q+ 51.Kg2 Qg4+ 52.Kf1 Qc4+ 53.Kg2 e2 54.Kf2 ( 54.c8=Q Qxc8 55.Rxc8 e1=Q 56.Rf8 Qd2+ 57.Rf2 Qxd6 ) 54...Qe6 55.Ke1 Qxd6 56.Rh8+ Kg6 $1 ( 56...Kxh8 $4 57.c8=Q+ Kh7 58.Qc2+ ) 57.Rg8+ Kh5 { , 0-1. } ";
//		String movesList = "12.Bd3 Qb6 { Black has a wealth of good choices: } ( 12...Qh4+ 13.g3 Qe7 ) ( 12...Rb8 { and even } ) ( 12...O-O { since } 13.O-O { allows } 13...Qb6+ { picking up the b4-pawn. } ) 13.Rb1 O-O ( 13...Qe3+ 14.Ne2 g5 { undermining the e5-pawn is also good. } ) 14.Qe2 f6 ( 14...a5 $1 { is probably even stronger. I'm just trying to show that Black has far more dynamic options than White has. } ) 15.Qh5 f5 16.Qe2 Qd4 17.Qd2 a5 18.a3 axb4 19.axb4 Ra3 20.Ne2 Qa7 { and Black's in command (White's king is in the middle and Black's pawn breaks (...c6-c5 and ...g7-g5) are itching to be played. } ";
	String movesList = "23...Re7 $3 { White's troubles are exacerbated by the fact that he really has\n" +
			"no effective way of stopping Black's plan (...Rce8 followed by ...f5). Not\n" +
			"sensing the danger, Karpov continues playing in his characteristically\n" +
			"unhurried style. } ( 23...Nc5 $2 24.Bd4 $1 { and Black is forced to acquiesce\n" +
			"to the higly undesirable trade of dark-squared bishops: } 24...Bxd4 ( 24...e5 25.fxe5 Bxe5 ( 25...dxe5 26.Bxc5 bxc5 27.Qh4 h5 28.gxh6 Bh8 29.h7+ Kf8 30.Rhf3 ) 26.Qh4 h5 27.gxh6 Bxd4 28.Nxd4 Qe7 29.Qe1 { Black's king might\n" +
			"not be in immediate danger, but it is only a matter of time before White\n" +
			"regroups his forces and crashes through along the f-file. } ) 25.Nxd4 Qe7 26.f5 $1 e5 27.f6 Qf8 28.Nf5 $1 { with a fearsome attack. In fact, I cannot see how\n" +
			"Black actually survives for more than a few moves! } ) ( 23...f5 24.gxf6 Bxf6 25.Bd4 Rf8 { The computer is quite optimistic about Black's chances (he\n" +
			"evaluates the position as approximately equal), but I believe that it is\n" +
			"foolish to idolize Houdini in such a position. From a practical standpoint,\n" +
			"after } 26.Bxf6 Nxf6 27.Nd4 Rce8 28.Qh4 { I would certainly prefer White. } ) 24.Kg1 $6 { Karpov does little irreversible damage with this move, but it is\n" +
			"certainly a step in the wrong direction. In order to neutralize Black's\n" +
			"dangerous central operation, energetic and accurate play was required: } ( 24.Bd4 $1 Bxd4 25.Nxd4 e5 $1 26.fxe5 ( 26.Qh4 $5 Nf8 27.Nde2 exf4 28.Nxf4 Rce8 { Once again, White is not objectively worse, but Black's central pressure\n" +
			"guarantees him very comfortable play for the foreseeable future. } ) 26...dxe5 27.Nf3 ( 27.Nb3 Rf8 $1 28.Qd2 f6 ) 27...Rf8 28.Qh4 f6 29.gxf6 Nxf6 30.Ng5 Nh5 31.Rxf8+ Kxf8 32.Bf3 Nf6 { White maintains certain attacking\n" +
			"chances, but with excellent piece coordination and steady central pressure,\n" +
			"Black is certainly not worse. Of course, such a position did not suit Karpov\n" +
			"-  if he is circumspect, Black is hardly in danger of losing. } ) ( 24.Rd1 f5 25.gxf6 Nxf6 $5 26.Bxb6 Ng4 27.Qg1 Qb8 ) 24...Rce8 { Black follows through\n" +
			"on his plan (and, as a bonus, stops f4-f5 once and for all), but as Kasparov\n" +
			"notes, there was no need for such thorough preparation. The immediate } ( 24...f5 $1 { would have confronted White with serious problems. For instance, } 25.gxf6 Bxf6 26.Qd2 ( 26.f5 exf5 27.exf5 Bxg2 28.Qxg2 Qc6 { (Kasparov) } ) 26...Bg7 27.f5 exf5 28.exf5 Bxg2 29.Qxg2 Qc6 30.Qg5 Rce8 { and despite the\n" +
			"extreme complexity of the position, it appears that White can only level the\n" +
			"boat through a series of only computer moves. I have no interest in\n" +
			"over-analyzing this position: it is clear that ...f5 would have posed serious\n" +
			"objective problems. } ) 25.Rd1 f5 $1 { Black's plan has worked perfectly, but\n" +
			"White has improved his position just enough to neutralize the pressure. } 26.gxf6 Nxf6 { Bravely played! Black sacrifices a pawn, relying on the activity of\n" +
			"his pieces to serve as adequate compensation. Unwilling to calculate the\n" +
			"ramifications of 27.Bxb6, Karpov makes a neutral move: } 27.Rg3 { An indolent\n" +
			"move after which Kasparov's pieces come into play with savage effect. To be\n" +
			"sure, White's position is still very much playable, but the real test of\n" +
			"Black's strategy was the \"greedy\" } ( 27.Bxb6 $1 { After } 27...Ng4 28.Bxc7 Nxf2 29.Bxd6 Nxd1 30.Bxe7 Nxc3 $1 { During the game, Karpov feared } ( 30...Rxe7 31.Nxd1 Nxc2 { with seemingly powerful compensation for the pawn, but the\n" +
			"level-headed } 32.e5 $1 { , given by Kasparov, puts an end to Black's dreams.\n" +
			"Once again, I have no intention of boring you with a long investigation of the\n" +
			"position; suffice it to say that after } 32...Nb4 33.Nc3 Bxg2 34.Kxg2 Rf7 35.Rf3 g5 { Black has decent drawing chances, but this is certainly not the kind of\n" +
			"position Kasparov had in mind when sacrificing a pawn! } ) 31.bxc3 Nxc2 32.Bd6 { and now, according to Kasparov, the powerful } 32...e5 $1 { highlights the weakness\n" +
			"of White's central pawns and gives Black excellent drawing chances. Still, as\n" +
			"we will see, Karpov should have chosen this position over the text! } ) 27...Rf7 { Simple and strong. Kasparov leaves the b6 pawn under fire, but x-rays the\n" +
			"queen and prepares ...Nh5. Slowly but steadily, Karpov begins to lose control\n" +
			"of the game. } 28.Bxb6 Qb8 29.Be3 Nh5 $1 { A terrific resource. Kasparov\n" +
			"targets the weakest point in White's camp (the f4) pawn, and nearly forces a\n" +
			"repetition of moves. Needing a win, Karpov burns all the bridges and allows a\n" +
			"dazzling combinative sequence. } 30.Rg4 ( 30.Rf3 { For the longest time, I\n" +
			"could not, for the life of me, understand why Karpov did not make this\n" +
			"entirely natural move. It leaves the e4 pawn suspiciously weak, but Black\n" +
			"cannot exploit that... } 30...Bxc3 $1 31.bxc3 Na2 { Or can he? This sequence sure\n" +
			"looks computeresque, but of course Kasparov had to foresee it during the game.\n" +
			"Black has given up the pride of his bishop, but with the c3 and e4 pawns\n" +
			"hanging, White's position simply falls apart. } 32.Qd2 Qc7 $1 { Much stronger\n" +
			"than the rash } ( 32...Bxe4 $6 33.Ra1 $1 Nxc3 34.Qxc3 Bxf3 35.Bxf3 Nxf4 36.Nd4 { with good counterchances. } ) 33.Qxd6 Bxe4 34.Qxc7 Rxc7 35.Rf2 Nxc3 { White's pawn structure is dreadfully weak, his pieces are permanently\n" +
			"uncoordinated, and he can barely even move. Of course, going for such a\n" +
			"miserable position was out of the question for Karpov. } ) 30...Nf6 { Now, the\n" +
			"most prudent continuation would have been to repeat moves with 31.Rg3, but\n" +
			"Karpov decides to risk it all in a desperate attempt to muddy the waters. } 31.Rh4 $6 { Of course, given the circumstances, Karpov did not really have a\n" +
			"choice, but now Black tears open the f-file with great effect: } 31...g5 $1 32.fxg5 Ng4 ( 32...Nxe4 $2 { This looks tempting at first, but in fact White weathers\n" +
			"the storm with } 33.Qe2 Nxc3 34.bxc3 Nd5 35.Rf1 $1 Rxf1+ 36.Bxf1 Nxe3 37.Qxe3 { and it turns out that White has developed some formidable attacking\n" +
			"chances connected with the threats of Bd3, Bc4, and Qh3. } ) 33.Qd2 Nxe3 34.Qxe3 { With the e3 bishop gone, White's position hangs on a thread. } 34...Nxc2 35.Qb6 Ba8 $1 { Admirable precision! The queen trade is clearly the objectively\n" +
			"best move for White, but, once again, you must remember the circumstances  -  a\n" +
			"draw for Karpov was tantamount to a loss. } 36.Rxd6 Rb7 37.Qxa6 Rxb3 $6 { Finally, the pressure begins to take its toll. This move is viable, but the\n" +
			"prosaic } ( 37...Nb4 $1 { won a rook on the spot. } ) 38.Rxe6 Rxb2 { Oddly enough,\n" +
			"this natural move actually allows a draw. After the more precise } ( 38...Ne3 $1 39.Rxe8+ Qxe8 40.Qe2 Rb4 $5 { , White would have remained a piece down for\n" +
			"little compensation. } ) 39.Qc4 ( 39.Nd1 ) 39...Kh8 40.e5 $2 { Karpov, in\n" +
			"despair, loses all hope and commits hara-kiri. In fact, the incredibly\n" +
			"cold-blooded } ( 40.Rxe8+ Qxe8 41.Nd1 $1 { would have saved the game (but not\n" +
			"the match, of course, so this is technically a moot point): } 41...Na3 42.Qd3 Ra2 43.g6 $1 h6 { and now the improbable } 44.Rxh6+ $1 { achieves perpetual: } 44...Bxh6 45.Qc3+ Bg7 ( 45...Kg8 46.Qb3+ Kg7 47.Qxa2 Qd7 48.Nf2 { and White can\n" +
			"suddenly play for a win! } ) 46.Qh3+ Kg8 47.Qb3+ Kf8 48.Qb4+ Qe7 49.Qb8+ Qe8 50.Qb4+ ) 40...Qa7+ $1 { An elegant finishing touch. White's position\n" +
			"comes apart at the seams. } 41.Kh1 Bxg2+ 42.Kxg2 Nd4+ ";
}
