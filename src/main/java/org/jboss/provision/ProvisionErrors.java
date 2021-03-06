/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.provision;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipException;

import javax.xml.stream.XMLStreamException;

import org.jboss.provision.util.HashUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisionErrors {

    public static String nullArgument(String name) {
        return name + " is null";
    }

    public static String couldNotCreateDir(File dir) {
        return "Could not create directory " + dir.getAbsolutePath();
    }

    public static String notADir(File f) {
        return "Not a directory: " + f.getAbsolutePath();
    }

    public static ProvisionException hashCalculationFailed(String path, IOException e) {
        return new ProvisionException("Failed to calculate hash for " + path, e);
    }

    public static ProvisionException pathDoesNotExist(File f) {
        return new ProvisionException("Content path " + f.getAbsolutePath() + " does not exist.");
    }

    public static ProvisionException unexpectedContentHash(File f, byte[] expectedHash, byte[] actualHash) {
        return new ProvisionException("Expected hash for " + f.getAbsolutePath() + " is " + HashUtils.bytesToHexString(expectedHash) +
                " but was " + HashUtils.bytesToHexString(actualHash));
    }

    public static ProvisionException failedToZip(File f, IOException e) {
        return new ProvisionException("Failed to zip content of " + f.getAbsolutePath(), e);
    }

    public static ProvisionException unitNamesDoNotMatch(String expected, String actual) {
        return new ProvisionException("Expected unit name " + expected + " but the target is " + actual);
    }

    public static ProvisionException xmlMarshallingFailed(String xmlName, Exception e) {
        return new ProvisionException("Failed to marshal " + xmlName, e);
    }

    public static ProvisionException zipFormatError(File zipFile, ZipException e) {
        return new ProvisionException("Failed to read ZIP archive " + zipFile.getAbsolutePath(), e);
    }

    public static ProvisionException readError(File f, IOException e) {
        return new ProvisionException("Failed to read from " + f.getAbsolutePath(), e);
    }

    public static ProvisionException writeError(File f, IOException e) {
        return new ProvisionException("Failed to write to " + f.getAbsolutePath(), e);
    }

    public static ProvisionException failedToParse(String xml, XMLStreamException e) {
        return new ProvisionException("Failed to parse " + xml, e);
    }

    public static ProvisionException unitAlreadyInstalled(String name, String version) {
        return new ProvisionException("Unit already installed " + name + "-" + version);
    }

    public static ProvisionException unitIsNotInstalled(String name) {
        return new ProvisionException("Unit is not installed " + name);
    }

    public static ProvisionException unitVersionMismatch(String name, String expectedVersion, String actualVersion) {
        return new ProvisionException("The installed version of " + name + " is " + actualVersion + " but the provisioning task targets version " + expectedVersion);
    }

    public static ProvisionException pathAlreadyExists(File path) {
        return new ProvisionException("Content path " + path + " already exists.");
    }

    public static ProvisionException pathHashMismatch(File path, String expectedHash, String actualHash) {
        return new ProvisionException("The hash of " + path + " is " + actualHash + " but expected to be " + expectedHash);
    }

    public static ProvisionException pathHashMismatch(String path, String expectedHash, String actualHash) {
        return new ProvisionException("The hash of " + path + " is " + actualHash + " but expected to be " + expectedHash);
    }

    public static ProvisionException unitHomeNotDefined(String unitName) {
        return new ProvisionException("Unit home path not defined for " + unitName);
    }

    public static ProvisionException undefinedNamedLocation(String namedLocation) {
        return new ProvisionException("Undefined named location: " + namedLocation);
    }

    public static ProvisionException deleteFailed(File f) {
        return new ProvisionException("Could not delete " + f.getAbsolutePath());
    }

    public static ProvisionException patchCantChangeVersion() {
        return new ProvisionException("Patch is not allowed to install new or change the version of the installed software.");
    }

    public static ProvisionException patchIdMissing() {
        return new ProvisionException("Patch ID is required to produce a patch");
    }

    public static ProvisionException pathCopiedAndDeleted(String path) {
        return new ProvisionException(path + " is copied and deleted during the same file system update.");
    }

    public static ProvisionException pathCopiedMoreThanOnce(String path) {
        return new ProvisionException(path + " is copied more than once during the same file system update.");
    }

    public static ProvisionException failedToStoreEnvironment(Throwable t) {
        return new ProvisionException("Failed to store provision environment", t);
    }

    public static ProvisionException failedToLoadEnvironmentRecord(Throwable t) {
        return new ProvisionException("Failed to load provision environment record", t);
    }

    public static String relativePathMissing() {
        return "relative path is missing";
    }

    public static String contentHashMissing() {
        return "content hash is missing";
    }

    public static ProvisionException failedToCopy(String path, File target) {
        return new ProvisionException("Failed to copy " + path + " to " + target.getAbsolutePath());
    }

    public static ProvisionException failedToDelete(File target) {
        return new ProvisionException("Failed to delete " + target.getAbsolutePath());
    }

    public static ProvisionException failedToCopyContentFromZIP(String path, File targetFile, IOException e) {
        return new ProvisionException("Failed to copy content from ZIP path " + path + " to " + targetFile.getAbsolutePath(), e);
    }

    public static ProvisionException unknownEnvironmentProperty(String prop) {
        return new ProvisionException("Unknown provision environment property: " + prop);
    }

    public static ProvisionException unknownUnit(String unitName) {
        return new ProvisionException("Unknown unit " + unitName);
    }

    public static ProvisionException environmentHasNoUnits() {
        return new ProvisionException("Environment has no units");
    }

    public static ProvisionException failedToUpdateHistory(Throwable t) {
        return new ProvisionException("Failed to update environment history", t);
    }

    public static ProvisionException packageDoesNotAffectUnits() {
        return new ProvisionException("Package does not affect any unit.");
    }

    public static ProvisionException environmentAlreadyExists(File envHome) {
        return new ProvisionException("Environment already exists at " + envHome.getAbsolutePath());
    }

    public static ProvisionException instructionIsNotAssociatedWithFile() {
        return new ProvisionException("Instruction is not associated with a persistent file.");
    }

    public static ProvisionException noHistoryRecordedUntilThisPoint() {
        return new ProvisionException("No history recorded until this point.");
    }

    public static ProvisionException instructionTargetsOtherThanRequestedUnits(String targetUnit, Set<String> affectedUnits) {
        return new ProvisionException("Can't clean up history for unit " + targetUnit +
                " as one of the instructions applied affects other units too " + affectedUnits);
    }

    public static ProvisionException historyRecordMissingForInstruction(String unitName, String id) {
        return new ProvisionException(unitName + " history is missing a record for environment instruction " + id);
    }

    public static ProvisionException versionUpdateOverPatch(String unitName, String patchId, String updatedVersion) {
        return new ProvisionException("Before " + updatedVersion + " of " + unitName + " can be installed, patch " + patchId + " has to be reverted.");
    }

    public static ProvisionException userDoesNotOwnTargetPath(String author, String path) {
        return new ProvisionException(author + " does not own " + path);
    }

    public static ProvisionException backupFailed(Throwable e) {
        return new ProvisionException("Failed to back up content", e);
    }

    public static ProvisionException failedToCopyContent(Throwable e) {
        return new ProvisionException("Failed to copy content", e);
    }

    public static ProvisionException unexpectedTaskFormat() {
        return new ProvisionException("Unexpected task format");
    }

    public static ProvisionException sessionRecordMissingForPath(String sessionId, String relativePath) {
        return new ProvisionException("Session record " + sessionId + " is missing for path " + relativePath);
    }
}
