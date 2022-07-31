package de.hetzge.eclipse.flix.internal.refactoring;

import org.lxtk.lx4e.refactoring.CreateResourceParticipant;
import org.lxtk.lx4e.refactoring.DeleteResourceParticipant;
import org.lxtk.lx4e.refactoring.IFileOperationParticipantSupport;
import org.lxtk.lx4e.refactoring.MoveResourceParticipant;
import org.lxtk.lx4e.refactoring.RenameResourceParticipant;

import de.hetzge.eclipse.flix.FlixCore;

public final class FlixResourceParticipant {

	private FlixResourceParticipant() {
	}

	public static class Create extends CreateResourceParticipant {

		@Override
		protected IFileOperationParticipantSupport getFileOperationParticipantSupport() {
			return FlixCore.FILE_OPERATION_PARTICIPANT_SUPPORT;
		}

		@Override
		public String getName() {
			return "Flix create participant";
		}
	}

	public static class Delete extends DeleteResourceParticipant {

		@Override
		protected IFileOperationParticipantSupport getFileOperationParticipantSupport() {
			return FlixCore.FILE_OPERATION_PARTICIPANT_SUPPORT;
		}

		@Override
		public String getName() {
			return "Flix delete participant";
		}
	}

	public static class Rename extends RenameResourceParticipant {

		@Override
		protected IFileOperationParticipantSupport getFileOperationParticipantSupport() {
			return FlixCore.FILE_OPERATION_PARTICIPANT_SUPPORT;
		}

		@Override
		public String getName() {
			return "Flix rename participant";
		}
	}

	public static class Move extends MoveResourceParticipant {

		@Override
		protected IFileOperationParticipantSupport getFileOperationParticipantSupport() {
			return FlixCore.FILE_OPERATION_PARTICIPANT_SUPPORT;
		}

		@Override
		public String getName() {
			return "Flix move participant";
		}
	}
}
