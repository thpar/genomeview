<?php
$baseurl = substr($_SERVER["SCRIPT_URI"], 0, strripos($_SERVER["SCRIPT_URI"], "/") + 1);
$basedir = substr($_SERVER["SCRIPT_FILENAME"], 0, strripos($_SERVER["SCRIPT_FILENAME"], "/") + 1);

$tracks = dirList($basedir . ".");

/* We very strongly recommend using the central GenomeView release. 
 * This version is always up to date and is distributed through the
 * global Sourceforge download mirror network */ 
$starturl='http://genomeview.org/start/snapshot.jnlp?--config ' . $baseurl . 'genomeview.txt';

$all = "";
foreach ($tracks as $track) {
	$all .= " " . $baseurl . $track;
}
echo $all;
$starturl.=$all;
header("Location: $starturl");



function endsWith( $str, $sub ) {
return ( substr( $str, strlen( $str ) - strlen( $sub ) ) == $sub );
}
function dirList($directory) {

	// create an array to hold directory list
	$results = array ();

	// create a handler for the directory
	$handler = opendir($directory);

	// keep going until all files in directory have been read
	while ($file = readdir($handler)) {
		/* 
		 * Files that will be excluded:
		 * this directory (.) and the parent (..) 
		 * index.php
		 * .htaccess
		 * All *.bam files, the bai will be included
		 */
				if (!is_dir($file)&&$file != '.' && $file != '..' && $file != '.htaccess'&&!endsWith($file,".html")&&!endsWith($file,".php")&&!endsWith($file,".fai")&&!endsWith($file,".tbi")&&!endsWith($file,".bam")&& $file != 'genomeview.txt'&& $file != 'index.php')
			$results[] = $file;
	}

	// tidy up: close the handler
	closedir($handler);

	// done!
	return $results;

}
?>
