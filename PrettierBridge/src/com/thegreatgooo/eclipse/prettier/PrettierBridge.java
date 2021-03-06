package com.thegreatgooo.eclipse.prettier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class PrettierBridge {

	private Optional<Process> npmProcess = Optional.empty();
	private ProcessHandle nodeChild;
	private AtomicReference<Path> bridgePath;
	private Path nodePath;
	private Path npmPath;
	private String[] envVars;
	private AtomicReference<Path> windowsKillPath;
	private final String pluginName;

	public PrettierBridge(AtomicReference<Path> bridgePath, Path nodePath, Path npmPath, String[] envVars,
			AtomicReference<Path> windowsKillPath, String pluginName) {
		this.bridgePath = bridgePath;
		this.nodePath = nodePath;
		this.envVars = envVars;
		this.windowsKillPath = windowsKillPath;
		this.npmPath = npmPath;
		this.pluginName = pluginName;
	}

	public String getFormattedCode(String unformattedCode)
			throws IOException, InterruptedException, URISyntaxException {
		if (!npmProcess.isPresent()) {
			startNpmProcess();
		}

		if (!npmProcess.get().isAlive()) {
			throw new RuntimeException("NPM process unexpectedly errored out");
		}

		interruptProcess(nodeChild.pid());

		waitForResponseOnErrorStream(npmProcess.get());
		npmProcess.get().getOutputStream().write(unformattedCode.getBytes("UTF8"));
		npmProcess.get().getOutputStream().flush();

		interruptProcess(nodeChild.pid());
		waitForResponseOnErrorStream(npmProcess.get());

		StringBuilder formattedCode = new StringBuilder();
		while (npmProcess.get().getInputStream().available() > 0) {
			formattedCode.append(new String(
					npmProcess.get().getInputStream().readNBytes(npmProcess.get().getInputStream().available())));
		}
		if (formattedCode.length() > 0 && formattedCode.charAt(formattedCode.length() - 1) == '\n') {
			return formattedCode.substring(0, formattedCode.length() - 1);
		}
		if (formattedCode.length() == 0) {
			return unformattedCode;
		}
		return formattedCode.toString();
	}

	public void close() {
		if (npmProcess.isPresent()) {
			npmProcess.get().destroy();
		}
	}

	private synchronized void startNpmProcess() throws IOException, InterruptedException {
		copyBridgeToTemp();
		copyWindowsKillToTemp();

		String[] command = { npmPath.toString(), "run", "bridge" };
		npmProcess = Optional.of(Runtime.getRuntime().exec(command, envVars, bridgePath.get().toFile()));
		Instant processStartTime = Instant.now();
		StringBuilder secondToLastLine = new StringBuilder();
		StringBuilder lastLine = new StringBuilder();
		while (Duration.between(processStartTime, Instant.now()).getSeconds() < 5) {
			if (npmProcess.get().getInputStream().available() == 0) {
				Thread.sleep(100);
			}
			String npmProcessOutput = new String(
					npmProcess.get().getInputStream().readNBytes(npmProcess.get().getInputStream().available()));
			int newLineIndex = npmProcessOutput.lastIndexOf("\n");
			if (newLineIndex == -1) {
				lastLine.append(npmProcessOutput);
			} else {
				secondToLastLine.setLength(0);
				secondToLastLine.append(lastLine.toString());
				secondToLastLine.append(npmProcessOutput.substring(0, newLineIndex));
				int indexOfSecondToLastNewLineIndex = npmProcessOutput.lastIndexOf('\n', newLineIndex - 1);
				if (indexOfSecondToLastNewLineIndex != -1) {
					secondToLastLine.setLength(0);
					secondToLastLine
							.append(npmProcessOutput.substring(indexOfSecondToLastNewLineIndex + 1, newLineIndex));
				}
				lastLine.setLength(0);
				lastLine.append(npmProcessOutput.substring(newLineIndex + 1));
			}
			if (secondToLastLine.toString().equals(">READY<")) {
				break;
			}
		}
		if (!secondToLastLine.toString().equals(">READY<")) {
			throw new RuntimeException("npm bridge had issues starting");
		}
		if (npmProcess.get().getErrorStream().available() > 0) {
			System.out.println("Unexpected crud in error Stream");
			System.out.println(new String(
					npmProcess.get().getErrorStream().readNBytes(npmProcess.get().getErrorStream().available())));
		}

		nodeChild = findChildProcess(npmProcess.get()).get();
		while (nodeChild.children().count() > 0) {
			nodeChild = nodeChild.children().findAny().get();
		}

	}

	private void copyWindowsKillToTemp() throws IOException {
		if (windowsKillPath.get() == null) {
			synchronized (windowsKillPath) {
				if (windowsKillPath.get() == null) {
					windowsKillPath.set(copyResourceToTemp("/windows/windows-kill.exe", "kill", ".exe"));
				}
			}
		}
	}

	private void copyBridgeToTemp() throws IOException, InterruptedException {
		if (bridgePath.get() == null) {
			synchronized (bridgePath) {
				if (bridgePath.get() == null) {
					bridgePath.set(copyBridge());
				}
			}
		}
	}

	private Path copyBridge() throws IOException, InterruptedException {
		Path bridgeInstallPath = Files.createTempDirectory("bridge-js");
		Files.createDirectories(bridgeInstallPath.resolve("js/scripts/"));
		copyResourceToPath("/js/scripts/bridge.js", bridgeInstallPath.resolve("js/scripts/bridge.js"));
		copyResourceToPath("/js/package.json", bridgeInstallPath.resolve("js/package.json"));
		String[] command = { npmPath.toString(), "install" };
		Process process = Runtime.getRuntime().exec(command, envVars,
				bridgeInstallPath.resolve("js").toAbsolutePath().toFile());
		process.waitFor();
		System.out.println(new String(process.getErrorStream().readNBytes(100)));
		return bridgeInstallPath.resolve("js");
	}

	private Path copyResourceToTemp(String resourceName, String prefix, String suffix) throws IOException {
		Path temporaryFile = File.createTempFile(prefix, suffix).toPath();
		copyResourceToPath(resourceName, temporaryFile);
		return temporaryFile;
	}

	private void copyResourceToPath(String resourceName, Path temporaryFile) throws IOException {
		URL resourceUrl = new URL(String.format("platform:/plugin/%s/resource/%s", pluginName, resourceName));
		try (InputStream is = resourceUrl.openStream()) {
			Files.copy(is, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private Optional<ProcessHandle> findChildProcess(Process p) {
		return p.descendants().filter(child -> child.children().count() == 0)
				.filter(child -> child.info().command().get().equals(nodePath.toString())).findAny();
	}

	private void interruptProcess(long pid) throws IOException, URISyntaxException {
		if (isWindowsOs()) {
			Runtime.getRuntime().exec(windowsKillPath.get().toAbsolutePath() + " -SIGINT " + pid);
		} else {
			Runtime.getRuntime().exec("kill -SIGINT " + pid);
		}
	}

	private boolean isWindowsOs() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	private void waitForResponseOnErrorStream(Process p) throws IOException, InterruptedException {
		Instant startTime = Instant.now();
		while (p.getErrorStream().available() == 0) {
			Thread.sleep(1);
			if (Duration.between(startTime, Instant.now()).getSeconds() > 1) {
				throw new RuntimeException("Prettier bridge took too long to return status code");
			}
		}
	}

}
